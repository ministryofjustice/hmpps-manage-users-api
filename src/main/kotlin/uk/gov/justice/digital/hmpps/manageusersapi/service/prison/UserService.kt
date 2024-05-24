package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.EnhancedPrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonAdminUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonStaffUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserSearchSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.filter.PrisonUserFilter
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedCentralAdminUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedGeneralUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedLocalAdminUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType.DPS_GEN
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.EntityNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailService

@Service("PrisonUserService")
class UserService(
  private val prisonUserApiService: UserApiService,
  private val authApiService: AuthApiService,
  private val notificationService: NotificationService,
  private val verifyEmailDomainService: VerifyEmailDomainService,
  private val verifyEmailService: VerifyEmailService,
) {

  fun changeEmail(username: String, newEmailAddress: String): String {
    val prisonUser = prisonUserApiService.findUserByUsername(username)
    prisonUser?.let {
      authApiService.confirmRecognised(username)
      val verifyLinkEmailAndUsername = verifyEmailService.requestVerification(prisonUser, newEmailAddress)
      authApiService.updateEmail(username, verifyLinkEmailAndUsername.email)
      return verifyLinkEmailAndUsername.link
    } ?: throw EntityNotFoundException("Prison username $username not found")
  }

  fun enableUser(username: String) {
    val prisonUser = prisonUserApiService.findUserByUsername(username)
    prisonUser?.let {
      prisonUserApiService.enableUserByUserId(username)
    } ?: throw EntityNotFoundException("Prison username $username not found")
  }

  fun disableUser(username: String) {
    val prisonUser = prisonUserApiService.findUserByUsername(username)
    prisonUser?.let {
      prisonUserApiService.disableUserByUserId(username)
    } ?: throw EntityNotFoundException("Prison username $username not found")
  }

  @Throws(HmppsValidationException::class)
  fun createUser(user: CreateUserRequest): PrisonUser {
    if (!verifyEmailDomainService.isValidEmailDomain(user.email.substringAfter('@'))) {
      throw HmppsValidationException(user.email.substringAfter('@'), "Email domain not valid")
    }

    val prisonUserDetails = when (user.userType) {
      DPS_ADM -> prisonUserApiService.createCentralAdminUser(user)
      DPS_GEN -> prisonUserApiService.createGeneralUser(user)
      DPS_LSA -> prisonUserApiService.createLocalAdminUser(user)
    }
    notificationService.newPrisonUserNotification(user, "DPSUserCreate")
    return prisonUserDetails
  }

  fun findUserByUsername(username: String) = prisonUserApiService.findUserByUsernameWithError(username)

  fun findUserDetailsByUsername(username: String) = prisonUserApiService.findUserDetailsByUsername(username)

  fun createLinkedCentralAdminUser(linkUserRequest: CreateLinkedCentralAdminUserRequest): PrisonStaffUser {
    val prisonStaffUser = prisonUserApiService.linkCentralAdminUser(linkUserRequest)
    sendEmailNotification(linkUserRequest.adminUsername, DPS_ADM, prisonStaffUser)
    return prisonStaffUser
  }

  private fun sendEmailNotification(userName: String, userType: UserType, user: PrisonStaffUser) {
    user.primaryEmail?.let {
      val createUserRequest = CreateUserRequest(
        username = userName,
        email = it,
        firstName = user.firstName,
        lastName = user.lastName,
        userType = userType,
      )
      notificationService.newPrisonUserNotification(createUserRequest, "DPLinkUserCreate")
    }
  }

  fun createLinkedLocalAdminUser(linkUserRequest: CreateLinkedLocalAdminUserRequest): PrisonStaffUser {
    val prisonStaffUser = prisonUserApiService.linkLocalAdminUser(linkUserRequest)
    sendEmailNotification(linkUserRequest.adminUsername, DPS_LSA, prisonStaffUser)
    return prisonStaffUser
  }

  fun createLinkedGeneralUser(linkUserRequest: CreateLinkedGeneralUserRequest): PrisonStaffUser {
    val prisonStaffUser = prisonUserApiService.linkGeneralUser(linkUserRequest)
    sendEmailNotification(linkUserRequest.generalUsername, DPS_GEN, prisonStaffUser)

    return prisonStaffUser
  }

  fun findUsersByFirstAndLastName(firstName: String, lastName: String): List<EnhancedPrisonUser> {
    val prisonUsers: List<PrisonUserSummary> = prisonUserApiService.findUsersByFirstAndLastName(firstName, lastName)
    // The users may have an unverified email, so we need to go to auth to determine if they are different
    if (prisonUsers.isNotEmpty()) {
      val authUsersByUsername = authApiService
        .findUserEmails(prisonUsers.map { it.username })
        .filter { !it.email.isNullOrBlank() }
        .associateBy { it.username }

      return prisonUsers.map {
        EnhancedPrisonUser(
          username = it.username,
          userId = it.staffId,
          email = authUsersByUsername[it.username]?.email ?: it.email,
          verified = authUsersByUsername[it.username]?.verified ?: (it.email != null),
          firstName = it.firstName,
          lastName = it.lastName,
          activeCaseLoadId = it.activeCaseload?.id,
        )
      }
    }
    return listOf()
  }

  fun findUsersByFilter(pageRequest: Pageable, filter: PrisonUserFilter): PagedResponse<PrisonUserSearchSummary> {
    return prisonUserApiService.findUsersByFilter(pageRequest, filter)
  }

  fun downloadUsersByFilter(filter: PrisonUserFilter): List<PrisonUserSummary> {
    return prisonUserApiService.downloadUsersByFilter(filter)
  }

  fun downloadPrisonAdminsByFilter(filter: PrisonUserFilter): List<PrisonAdminUserSummary> {
    return prisonUserApiService.downloadPrisonAdminsByFilter(filter)
  }
}

class HmppsValidationException(emailDomain: String, errorCode: String) :
  Exception("Invalid Email domain: $emailDomain with reason: $errorCode")

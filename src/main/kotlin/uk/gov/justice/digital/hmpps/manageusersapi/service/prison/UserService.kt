package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.NewPrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType.DPS_GEN
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.EntityNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailService

@Service("NomisUserService")
class UserService(
  private val prisonUserApiService: UserApiService,
  private val authApiService: AuthApiService,
  private val notificationService: NotificationService,
  private val verifyEmailDomainService: VerifyEmailDomainService,
  private val verifyEmailService: VerifyEmailService
) {

  fun changeEmail(username: String, newEmailAddress: String): String {
    val prisonUser = prisonUserApiService.findUserByUsername(username)
    prisonUser?.let {

      if(!authApiService.recognised(username)) {
        throw EntityNotFoundException("Username $username not recognised")
      }

      val verifyLinkEmailAndUsername = verifyEmailService.requestVerification(prisonUser, newEmailAddress)
      authApiService.updateEmail(username, verifyLinkEmailAndUsername.email)
      return verifyLinkEmailAndUsername.link

    } ?: throw EntityNotFoundException("Prison username $username not found")
  }

  @Throws(HmppsValidationException::class)
  fun createUser(user: CreateUserRequest): NewPrisonUser {
    if (!verifyEmailDomainService.isValidEmailDomain(user.email.substringAfter('@'))) {
      throw HmppsValidationException(user.email.substringAfter('@'), "Email domain not valid")
    }

    val nomisUserDetails = when (user.userType) {
      DPS_ADM -> prisonUserApiService.createCentralAdminUser(user)
      DPS_GEN -> prisonUserApiService.createGeneralUser(user)
      DPS_LSA -> prisonUserApiService.createLocalAdminUser(user)
    }
    notificationService.newPrisonUserNotification(user, "DPSUserCreate")
    return nomisUserDetails
  }

  fun findUsersByFirstAndLastName(firstName: String, lastName: String): List<PrisonUser> {
    val nomisUsers: List<PrisonUserSummary> = prisonUserApiService.findUsersByFirstAndLastName(firstName, lastName)
    // The users may have an unverified email, so we need to go to auth to determine if they are different
    if (nomisUsers.isNotEmpty()) {
      val authUsersByUsername = authApiService
        .findUserEmails(nomisUsers.map { it.username })
        .filter { !it.email.isNullOrBlank() }
        .associateBy { it.username }

      return nomisUsers.map {
        PrisonUser(
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
}

class HmppsValidationException(emailDomain: String, errorCode: String) :
  Exception("Invalid Email domain: $emailDomain with reason: $errorCode")

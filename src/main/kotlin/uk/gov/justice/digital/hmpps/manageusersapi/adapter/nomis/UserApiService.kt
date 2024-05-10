package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonStaffUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserBasicDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedCentralAdminUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedGeneralUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedLocalAdminUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.service.EntityNotFoundException

@Service(value = "nomisUserApiService")
class UserApiService(
  @Qualifier("nomisWebClientUtils") val serviceWebClientUtils: WebClientUtils,
  @Qualifier("nomisUserWebClientUtils") val userWebClientUtils: WebClientUtils,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createCentralAdminUser(centralAdminUser: CreateUserRequest): PrisonUser {
    log.debug("Create DPS central admin user - {}", centralAdminUser.username)
    return userWebClientUtils.postWithResponse(
      "/users/admin-account",
      mapOf(
        "username" to centralAdminUser.username,
        "email" to centralAdminUser.email,
        "firstName" to centralAdminUser.firstName,
        "lastName" to centralAdminUser.lastName,
      ),
      PrisonUser::class.java,
      HttpStatus.CONFLICT,
      UserExistsException(centralAdminUser.username),
    )
  }

  fun createGeneralUser(generalUser: CreateUserRequest): PrisonUser {
    log.debug("Create DPS general user - {}", generalUser.username)
    return userWebClientUtils.postWithResponse(
      "/users/general-account",
      mapOf(
        "username" to generalUser.username,
        "email" to generalUser.email,
        "firstName" to generalUser.firstName,
        "lastName" to generalUser.lastName,
        "defaultCaseloadId" to generalUser.defaultCaseloadId,
      ),
      PrisonUser::class.java,
      HttpStatus.CONFLICT,
      UserExistsException(generalUser.username),
    )
  }

  fun createLocalAdminUser(localAdminUser: CreateUserRequest): PrisonUser {
    log.debug("Create DPS local admin user - {}", localAdminUser.username)
    return userWebClientUtils.postWithResponse(
      "/users/local-admin-account",
      mapOf(
        "username" to localAdminUser.username,
        "email" to localAdminUser.email,
        "firstName" to localAdminUser.firstName,
        "lastName" to localAdminUser.lastName,
        "localAdminGroup" to localAdminUser.defaultCaseloadId,
      ),
      PrisonUser::class.java,
      HttpStatus.CONFLICT,
      UserExistsException(localAdminUser.username),
    )
  }

  fun findUserByUsername(username: String): PrisonUser? {
    if ("@" in username) {
      log.debug("Nomis not called with username as contained @: {}", username)
      return null
    }
    return serviceWebClientUtils.getIgnoreError("/users/{username}", PrisonUser::class.java, username.uppercase())
  }

  fun findUserBasicDetailsByUsername(username: String): PrisonUserBasicDetails? {
    if ("@" in username) {
      log.debug("Nomis not called with username as contained @: {}", username)
      return null
    }
    return serviceWebClientUtils.getIgnoreError("/users/basic/{username}", PrisonUserBasicDetails::class.java, username.uppercase())
  }

  fun findUserByUsernameWithError(username: String): PrisonUser? {
    if ("@" in username) {
      log.error("Nomis not called with username as contained @: {}", username)
      throw EntityNotFoundException("Prison username $username not allowed")
    }
    return serviceWebClientUtils.get("/users/{username}", PrisonUser::class.java, username.uppercase())
  }

  fun findUsersByFirstAndLastName(firstName: String, lastName: String): List<PrisonUserSummary> {
    return userWebClientUtils.getWithParams(
      "/users/staff",
      PrisonUserList::class.java,
      mapOf(
        "firstName" to firstName,
        "lastName" to lastName,
      ),
    )
  }

  fun linkCentralAdminUser(centralAdminUser: CreateLinkedCentralAdminUserRequest) = userWebClientUtils.postWithResponse(
    "/users/link-admin-account/{centralAdminUser}",
    mapOf(
      "username" to centralAdminUser.adminUsername,
    ),
    PrisonStaffUser::class.java,
    centralAdminUser.existingUsername,
  )

  fun linkLocalAdminUser(localAdminUser: CreateLinkedLocalAdminUserRequest) = userWebClientUtils.postWithResponse(
    "/users/link-local-admin-account/{localAdminUser}",
    mapOf(
      "username" to localAdminUser.adminUsername,
      "localAdminGroup" to localAdminUser.localAdminGroup,
    ),
    PrisonStaffUser::class.java,
    localAdminUser.existingUsername,
  )

  fun linkGeneralUser(generalUser: CreateLinkedGeneralUserRequest) = userWebClientUtils.postWithResponse(
    "/users/link-general-account/{generalUser}",
    mapOf(
      "username" to generalUser.generalUsername,
      "defaultCaseloadId" to generalUser.defaultCaseloadId,
    ),
    PrisonStaffUser::class.java,
    generalUser.existingAdminUsername,
  )

  fun enableUserByUserId(username: String) = userWebClientUtils.put(
    "/users/{username}/unlock-user",
    username,
  )

  fun disableUserByUserId(username: String) = userWebClientUtils.put(
    "/users/{username}/lock-user",
    username,
  )
}

class PrisonUserList : MutableList<PrisonUserSummary> by ArrayList()

class UserExistsException(username: String) :
  Exception("Unable to create user: username $username already exists")

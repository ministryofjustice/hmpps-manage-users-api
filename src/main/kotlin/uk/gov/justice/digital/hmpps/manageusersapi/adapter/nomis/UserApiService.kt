package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest

@Service(value = "nomisUserApiService")
class UserApiService(
  @Qualifier("nomisWebClientUtils") val serviceWebClientUtils: WebClientUtils,
  @Qualifier("nomisUserWebClientUtils") val userWebClientUtils: WebClientUtils,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createCentralAdminUser(centralAdminUser: CreateUserRequest): NomisUser {
    log.debug("Create DPS central admin user - {}", centralAdminUser.username)
    return userWebClientUtils.postWithResponse(
      "/users/admin-account",
      mapOf(
        "username" to centralAdminUser.username,
        "email" to centralAdminUser.email,
        "firstName" to centralAdminUser.firstName,
        "lastName" to centralAdminUser.lastName,
      ),
      NomisUser::class.java,
      HttpStatus.CONFLICT,
      UserExistsException(centralAdminUser.username),
    )
  }

  fun createGeneralUser(generalUser: CreateUserRequest): NomisUser {
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
      NomisUser::class.java,
      HttpStatus.CONFLICT,
      UserExistsException(generalUser.username),
    )
  }

  fun createLocalAdminUser(localAdminUser: CreateUserRequest): NomisUser {
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
      NomisUser::class.java,
      HttpStatus.CONFLICT,
      UserExistsException(localAdminUser.username),
    )
  }

  fun findUserByUsername(username: String): NomisUser? {
    if ("@" in username) {
      log.debug("Nomis not called with username as contained @: {}", username)
      return null
    }
    return serviceWebClientUtils.getIgnoreError("/users/${username.uppercase()}", NomisUser::class.java)
  }

  fun findUsersByFirstAndLastName(firstName: String, lastName: String): List<PrisonUserSummary> {
    return userWebClientUtils.getWithParams(
      "/users/staff",
      NomisUserList::class.java,
      mapOf(
        "firstName" to firstName,
        "lastName" to lastName,
      ),
    )
  }
}

class NomisUserList : MutableList<PrisonUserSummary> by ArrayList()

class UserExistsException(username: String) :
  Exception("Unable to create user: username $username already exists")

package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.NomisUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.NomisUserCreatedDetails

@Service(value = "nomisUserApiService")
class UserApiService(
  @Qualifier("nomisWebClientUtils") val serviceWebClientUtils: WebClientUtils,
  @Qualifier("nomisUserWebClientUtils") val userWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createCentralAdminUser(centralAdminUser: CreateUserRequest): NomisUserCreatedDetails {
    log.debug("Create DPS central admin user - {}", centralAdminUser.username)
    return userWebClientUtils.postWithResponse(
      "/users/admin-account",
      mapOf(
        "username" to centralAdminUser.username,
        "email" to centralAdminUser.email,
        "firstName" to centralAdminUser.firstName,
        "lastName" to centralAdminUser.lastName
      ),
      NomisUserCreatedDetails::class.java
    )
  }

  fun createGeneralUser(generalUser: CreateUserRequest): NomisUserCreatedDetails {
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
      NomisUserCreatedDetails::class.java
    )
  }

  fun createLocalAdminUser(localAdminUser: CreateUserRequest): NomisUserCreatedDetails {
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
      NomisUserCreatedDetails::class.java
    )
  }

  fun findUserByUsername(username: String): NomisUserDetails? {
    if ("@" in username) {
      log.debug("Nomis not called with username as contained @: {}", username)
      return null
    }
    return serviceWebClientUtils.getIgnoreError("/users/${username.uppercase()}", NomisUserDetails::class.java)
  }

  fun findUsersByFirstAndLastName(firstName: String, lastName: String): List<NomisUserSummaryDto> {
    return userWebClientUtils.getWithParams(
      "/users/staff", NomisUserList::class.java,
      mapOf(
        "firstName" to firstName,
        "lastName" to lastName
      )
    )
  }
}

class NomisUserList : MutableList<NomisUserSummaryDto> by ArrayList()

data class NomisUserSummaryDto(
  val username: String,
  val staffId: String,
  val firstName: String,
  val lastName: String,
  val active: Boolean,
  val activeCaseload: PrisonCaseload?,
  val email: String?,
)
data class PrisonCaseload(
  val id: String,
  val name: String,
)

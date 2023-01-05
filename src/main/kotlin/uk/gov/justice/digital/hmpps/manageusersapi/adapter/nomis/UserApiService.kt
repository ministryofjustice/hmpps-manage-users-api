package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.NomisUserDetails

@Service
class UserApiService(
  @Qualifier("nomisWebClientUtils") val nomisWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createCentralAdminUser(centralAdminUser: CreateUserRequest): NomisUserDetails {
    RolesApiService.log.debug("Create DPS central admin user - {}", centralAdminUser.username)
    return nomisWebClientUtils.postWithResponse(
      "/users/admin-account",
      mapOf(
        "username" to centralAdminUser.username,
        "email" to centralAdminUser.email,
        "firstName" to centralAdminUser.firstName,
        "lastName" to centralAdminUser.lastName
      ),
      NomisUserDetails::class.java
    )
  }

  fun createGeneralUser(generalUser: CreateUserRequest): NomisUserDetails {
    RolesApiService.log.debug("Create DPS general user - {}", generalUser.username)
    return nomisWebClientUtils.postWithResponse(
      "/users/general-account",
      mapOf(
        "username" to generalUser.username,
        "email" to generalUser.email,
        "firstName" to generalUser.firstName,
        "lastName" to generalUser.lastName,
        "defaultCaseloadId" to generalUser.defaultCaseloadId,
      ),
      NomisUserDetails::class.java
    )
  }

  fun createLocalAdminUser(localAdminUser: CreateUserRequest): NomisUserDetails {
    RolesApiService.log.debug("Create DPS local admin user - {}", localAdminUser.username)
    return nomisWebClientUtils.postWithResponse(
      "/users/local-admin-account",
      mapOf(
        "username" to localAdminUser.username,
        "email" to localAdminUser.email,
        "firstName" to localAdminUser.firstName,
        "lastName" to localAdminUser.lastName,
        "localAdminGroup" to localAdminUser.defaultCaseloadId,
      ),
      NomisUserDetails::class.java
    )
  }
}

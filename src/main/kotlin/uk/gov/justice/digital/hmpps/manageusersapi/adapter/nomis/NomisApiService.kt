package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserRoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.NomisUserDetails

@Service
class NomisApiService(
  @Qualifier("nomisWebClient") val nomisWebClient: WebClient,
  @Qualifier("nomisWebClientUtils") val nomisWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createCentralAdminUser(centralAdminUser: CreateUserRequest): NomisUserDetails {
    log.debug("Create DPS central admin user - {}", centralAdminUser.username)
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
    log.debug("Create DPS general user - {}", generalUser.username)
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
    log.debug("Create DPS local admin user - {}", localAdminUser.username)
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

  fun createRole(createRole: CreateRole) {
    log.debug("Create dps role for {} with {}", createRole.roleCode, createRole)
    return nomisWebClientUtils.post(
      "/roles",
      mapOf(
        "code" to createRole.roleCode,
        "name" to createRole.roleName.nomisRoleName(),
        "adminRoleOnly" to createRole.adminType.adminRoleOnly(),
      )
    )
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleName(roleCode: String, roleNameAmendment: RoleNameAmendment) {
    log.debug("Updating dps role name for {} with {}", roleCode, roleNameAmendment)
    try {
      nomisWebClient.put().uri("/roles/$roleCode")
        .bodyValue(
          mapOf(
            "name" to roleNameAmendment.roleName.nomisRoleName()
          )
        )
        .retrieve()
        .toBodilessEntity()
        .block()
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.NOT_FOUND)) RoleNotFoundException("get", roleCode, "notfound") else e
    }
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleAdminType(roleCode: String, roleAdminTypeAmendment: RoleAdminTypeAmendment) {
    log.debug("Updating dps role name for {} with {}", roleCode, roleAdminTypeAmendment)
    try {
      nomisWebClient.put().uri("/roles/$roleCode")
        .bodyValue(
          mapOf(
            "adminRoleOnly" to roleAdminTypeAmendment.adminType.adminRoleOnly()
          )
        )
        .retrieve()
        .toBodilessEntity()
        .block()
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.NOT_FOUND)) RoleNotFoundException("get", roleCode, "notfound") else e
    }
  }

  private fun String.nomisRoleName(): String = take(30)

  fun getUserRoles(username: String): UserRoleDetail {
    try {
      return nomisWebClient.get()
        .uri("/users/$username/roles")
        .retrieve()
        .bodyToMono(UserRoleDetail::class.java)
        .block()!!
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.NOT_FOUND)) UserNotFoundException("get", username, "notfound") else e
    }
  }

  private fun Set<AdminType>.adminRoleOnly(): Boolean = (AdminType.DPS_LSA !in this)
}

class UserNotFoundException(action: String, username: String, errorCode: String) :
  Exception("Unable to $action user: $username with reason: $errorCode")

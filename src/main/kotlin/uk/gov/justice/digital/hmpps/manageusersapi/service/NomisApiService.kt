package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserRoleDetail

@Service
class NomisApiService(
  @Qualifier("nomisWebClient") val nomisWebClient: WebClient
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Throws(RoleExistsException::class)
  fun createRole(createRole: CreateRole) {
    log.debug("Create dps role for {} with {}", createRole.roleCode, createRole)
    try {
      nomisWebClient.post().uri("/roles")
        .bodyValue(
          mapOf(
            "code" to createRole.roleCode,
            "name" to createRole.roleName.nomisRoleName(),
            "adminRoleOnly" to createRole.adminType.adminRoleOnly(),
          )
        )
        .retrieve()
        .toBodilessEntity()
        .block()
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.CONFLICT)) RoleExistsException(
        createRole.roleCode,
        "role code already exists"
      ) else e
    }
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

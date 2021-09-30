package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment

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
      nomisWebClient.post().uri("/api/access-roles")
        .bodyValue(
          mapOf(
            "roleCode" to createRole.roleCode,
            "roleName" to createRole.roleName,
            "roleFunction" to createRole.adminType.roleFunction(),
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
      nomisWebClient.put().uri("/api/access-roles")
        .bodyValue(
          mapOf(
            "roleCode" to roleCode,
            "roleName" to roleNameAmendment.roleName
          )
        )
        .retrieve()
        .toBodilessEntity()
        .block()
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.NOT_FOUND)) RoleNotFoundException("get", roleCode, "notfound") else e
    }
  }

  private fun Set<AdminType>.roleFunction(): String = (if (AdminType.DPS_LSA in this) "GENERAL" else "ADMIN")
}

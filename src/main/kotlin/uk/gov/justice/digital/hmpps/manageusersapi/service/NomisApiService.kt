package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateUserRequest
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

  @Throws(UserExistsException::class)
  fun createCentralAdminUser(centralAdminUser: CreateUserRequest): NomisUserDetails {
    log.debug("Create DPS central admin user - {}", centralAdminUser.username)
    try {
      return nomisWebClient.post().uri("/users/admin-account")
        .bodyValue(
          mapOf(
            "username" to centralAdminUser.username,
            "email" to centralAdminUser.email,
            "firstName" to centralAdminUser.firstName,
            "lastName" to centralAdminUser.lastName,
          )
        )
        .retrieve()
        .bodyToMono(NomisUserDetails::class.java)
        .block()!!
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.CONFLICT)) UserExistsException(
        centralAdminUser.username,
        "username already exists"
      ) else e
    }
  }

  @Throws(UserExistsException::class)
  fun createGeneralUser(generalUser: CreateUserRequest): NomisUserDetails {
    log.debug("Create DPS general user - {}", generalUser.username)
    try {
      return nomisWebClient.post().uri("/users/general-account")
        .bodyValue(
          mapOf(
            "username" to generalUser.username,
            "email" to generalUser.email,
            "firstName" to generalUser.firstName,
            "lastName" to generalUser.lastName,
            "defaultCaseloadId" to generalUser.defaultCaseloadId,
          )
        )
        .retrieve()
        .bodyToMono(NomisUserDetails::class.java)
        .block()!!
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.CONFLICT)) UserExistsException(
        generalUser.username,
        "username already exists"
      ) else e
    }
  }

  @Throws(UserExistsException::class)
  fun createLocalAdminUser(localAdminUser: CreateUserRequest): NomisUserDetails {
    log.debug("Create DPS local admin user - {}", localAdminUser.username)
    try {
      return nomisWebClient.post().uri("/users/local-admin-account")
        .bodyValue(
          mapOf(
            "username" to localAdminUser.username,
            "email" to localAdminUser.email,
            "firstName" to localAdminUser.firstName,
            "lastName" to localAdminUser.lastName,
            "localAdminGroup" to localAdminUser.defaultCaseloadId,
          )
        )
        .retrieve()
        .bodyToMono(NomisUserDetails::class.java)
        .block()!!
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.CONFLICT)) UserExistsException(
        localAdminUser.username,
        "username already exists"
      ) else e
    }
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

  fun createRole(createRole: NomisRole) {
    log.debug("Create dps role for {} with {}", createRole.code, createRole)
    try {
      nomisWebClient.post().uri("/roles")
        .bodyValue(createRole)
        .retrieve()
        .toBodilessEntity()
        .block()
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.CONFLICT)) RoleExistsException(
        createRole.code,
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
  fun updateRole(roleCode: String, roleName: String?, adminRoleOnly: Boolean?) {
    log.debug("Updating dps role for {} with {}", roleCode)
    try {
      nomisWebClient.put().uri("/roles/$roleCode")
        .bodyValue(
          mapOf(
            "adminRoleOnly" to adminRoleOnly,
            "name" to roleName
          ).filter { it.value != null }
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

  fun getAllRoles(): List<NomisRole> {
    log.debug("Getting all dps roles")

    return nomisWebClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/roles/")
          .build()
      }
      .retrieve()
      .bodyToMono(RoleList::class.java)
      .block()!!
  }

  class RoleList : MutableList<NomisRole> by ArrayList()

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

  suspend fun getUsers(): List<NomisUser> =
    nomisWebClient.get()
      .uri { it.path("/users/emails").build() }
      .retrieve()
      .awaitBody()
}

class UserNotFoundException(action: String, username: String, errorCode: String) :
  Exception("Unable to $action user: $username with reason: $errorCode")

package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RolesPaged
import java.time.Duration

@Service
class AuthService(
  @Qualifier("authWebClient") val authWebClient: WebClient,
  @Qualifier("authWebWithClientId") val authWebWithClientId: WebClient,
  @Value("\${api.timeout:10s}")
  val timeout: Duration
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Throws(RoleExistsException::class)
  fun createRole(createRole: CreateRole) {
    log.debug("Create auth role for {} with {}", createRole.roleCode, createRole)
    try {
      authWebClient.post().uri("/api/roles")
        .bodyValue(
          mapOf(
            "roleCode" to createRole.roleCode,
            "roleName" to createRole.roleName,
            "roleDescription" to createRole.roleDescription,
            "adminType" to createRole.adminType.addDpsAdmTypeIfRequiredAsList(),
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
  fun getRoleDetail(roleCode: String): Role {
    try {
      return authWebClient.get()
        .uri("/api/roles/$roleCode")
        .retrieve()
        .bodyToMono(Role::class.java)
        .block(timeout) ?: throw RoleNotFoundException("get", roleCode, "notfound")
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.NOT_FOUND)) RoleNotFoundException("get", roleCode, "notfound") else e
    }
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    try {
      authWebClient.put()
        .uri("/api/roles/$roleCode")
        .bodyValue(roleAmendment)
        .retrieve()
        .toBodilessEntity()
        .block(timeout)
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.NOT_FOUND)) RoleNotFoundException("get", roleCode, "notfound") else e
    }
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    try {
      authWebClient.put()
        .uri("/api/roles/$roleCode/description")
        .bodyValue(roleAmendment)
        .retrieve()
        .toBodilessEntity()
        .block(timeout)
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.NOT_FOUND)) RoleNotFoundException("get", roleCode, "notfound") else e
    }
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    try {
      authWebClient.put()
        .uri("/api/roles/$roleCode/admintype")
        .bodyValue(mapOf("adminType" to roleAmendment.adminType.addDpsAdmTypeIfRequiredAsList()))
        .retrieve()
        .toBodilessEntity()
        .block(timeout)
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.NOT_FOUND)) RoleNotFoundException("get", roleCode, "notfound") else e
    }
  }

  @Throws(TokenException::class)
  fun createNewToken(createTokenRequest: CreateTokenRequest): String {
    log.debug("Create Token for user, {}", createTokenRequest.username)
    try {
      return authWebWithClientId.post().uri("/api/new-token")
        .bodyValue(
          mapOf(
            "username" to createTokenRequest.username,
            "email" to createTokenRequest.email,
            "source" to createTokenRequest.source
          )
        )
        .retrieve()
        .bodyToMono(String::class.java)
        .block()!!
    } catch (e: WebClientResponseException) {
      e.statusCode
      throw TokenException(createTokenRequest.username, e.statusCode.value())
    }
  }

  fun validateEmailDomain(emailDomain: String): Boolean {

    return authWebClient.get()
      .uri("/api/validate/email-domain?emailDomain=$emailDomain")
      .retrieve()
      .bodyToMono(Boolean::class.java)
      .block(timeout)!!
  }

  private fun Set<AdminType>.addDpsAdmTypeIfRequiredAsList() =
    (if (AdminType.DPS_LSA in this) (this + AdminType.DPS_ADM) else this).map { it.adminTypeCode }

  fun getPagedRoles(
    page: Int,
    size: Int,
    sort: String,
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?
  ): RolesPaged {

    return authWebClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/api/roles/paged")
          .queryParam("page", page)
          .queryParam("size", size)
          .queryParam("sort", sort)
          .queryParam("roleName", roleName)
          .queryParam("roleCode", roleCode)
          .queryParam("adminTypes", adminTypes)
          .build()
      }
      .retrieve()
      .bodyToMono(RolesPaged::class.java)
      .block()!!
  }

  fun getRoles(adminTypes: List<AdminType>?): List<Role> {

    return authWebClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/api/roles")
          .queryParam("adminTypes", adminTypes)
          .build()
      }
      .retrieve()
      .bodyToMono(RoleList::class.java)
      .block()!!
  }

  suspend fun getUsers(): List<AuthUser> =
    authWebClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/api/users/email")
          .queryParam("authSource", "nomis")
          .build()
      }
      .retrieve()
      .awaitBody()
}

class RoleList : MutableList<Role> by ArrayList()

class RoleNotFoundException(action: String, role: String, errorCode: String) :
  Exception("Unable to $action role: $role with reason: $errorCode")

class TokenException(userName: String, errorCode: Int) :
  Exception("Error creating token for user $userName, reason: $errorCode")

data class CreateTokenRequest(
  val username: String,
  val email: String,
  val source: String,
)

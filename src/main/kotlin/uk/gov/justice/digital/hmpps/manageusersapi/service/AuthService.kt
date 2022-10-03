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

  @Throws(TokenException::class)
  fun createNewToken(createTokenRequest: CreateTokenRequest): String {
    log.debug("Create Token for user, {}", createTokenRequest.username)
    try {
      return authWebWithClientId.post().uri("/api/new-token")
        .bodyValue(
          mapOf(
            "username" to createTokenRequest.username,
            "email" to createTokenRequest.email,
            "source" to createTokenRequest.source,
            "firstName" to createTokenRequest.firstName,
            "lastName" to createTokenRequest.lastName
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

class RoleNotFoundException(action: String, role: String, errorCode: String) :
  Exception("Unable to $action role: $role with reason: $errorCode")

class TokenException(userName: String, errorCode: Int) :
  Exception("Error creating token for user $userName, reason: $errorCode")

data class CreateTokenRequest(
  val username: String,
  val email: String,
  val source: String,
  val firstName: String,
  val lastName: String,
)

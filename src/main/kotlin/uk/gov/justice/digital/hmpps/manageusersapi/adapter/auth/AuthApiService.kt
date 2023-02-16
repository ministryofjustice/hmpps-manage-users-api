package uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto
import java.util.UUID

@Service
class AuthApiService(
  @Qualifier("authWebClientUtils") val serviceWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createNewToken(createTokenRequest: CreateTokenRequest): String {
    log.debug("Create Token for user, {}", createTokenRequest.username)
    return serviceWebClientUtils.postWithResponse(
      "/api/new-token",
      mapOf(
        "username" to createTokenRequest.username,
        "email" to createTokenRequest.email,
        "source" to createTokenRequest.source,
        "firstName" to createTokenRequest.firstName,
        "lastName" to createTokenRequest.lastName
      ),
      String::class.java
    )
  }

  fun findAzureUserByUsername(username: String): UserDetailsDto? =
    try {
      UUID.fromString(username)
      serviceWebClientUtils.getIgnoreError("/api/azureuser/$username", UserDetailsDto::class.java)
    } catch (exception: IllegalArgumentException) {
      log.debug("Auth not called for Azure user as username not valid UUID: {}", username)
      null
    }

  fun findUserByUsernameAndSource(username: String, source: AuthSource): AuthUserDetails =
    serviceWebClientUtils.getWithParams(
      "/api/user", AuthUserDetails::class.java,
      mapOf(
        "username" to username,
        "source" to source
      )
    )
}

data class CreateTokenRequest(
  val username: String,
  val email: String,
  val source: String,
  val firstName: String,
  val lastName: String,
)

data class AuthUserDetails(
  val uuid: UUID
)

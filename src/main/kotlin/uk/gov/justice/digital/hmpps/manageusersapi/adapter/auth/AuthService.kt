package uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service
class AuthService(
  @Qualifier("authWebWithClientId") val authWebWithClientId: WebClient,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
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
}

class TokenException(userName: String, errorCode: Int) :
  Exception("Error creating token for user $userName, reason: $errorCode")

data class CreateTokenRequest(
  val username: String,
  val email: String,
  val source: String,
  val firstName: String,
  val lastName: String,
)

package uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils

@Service
class AuthService(
  @Qualifier("authWebClientUtils") val authWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createNewToken(createTokenRequest: CreateTokenRequest): String {
    log.debug("Create Token for user, {}", createTokenRequest.username)
    return authWebClientUtils.postWithResponse(
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
}

data class CreateTokenRequest(
  val username: String,
  val email: String,
  val source: String,
  val firstName: String,
  val lastName: String,
)

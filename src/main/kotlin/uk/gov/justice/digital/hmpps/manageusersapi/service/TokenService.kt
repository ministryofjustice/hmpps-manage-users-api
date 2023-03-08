package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.CreateTokenRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException

@Service
class TokenService(
  private val notificationClient: NotificationClientApi,
  private val telemetryClient: TelemetryClient,
  private val authService: AuthApiService,
  @Value("\${hmpps-auth.endpoint.url}") val authBaseUri: String,
  @Value("\${application.notify.create-initial-password.template}") private val initialPasswordTemplateId: String,
) {

  @Throws(NotificationClientException::class)
  fun sendInitialPasswordEmail(
    user: CreateUserRequest,
    eventPrefix: String,
  ) {
    // Get token from Auth
    val token = authService.createNewToken(
      CreateTokenRequest(
        username = user.username,
        email = user.email, "nomis", firstName = user.firstName, lastName = user.lastName
      )
    )
    val passwordLink = getPasswordResetLink(token)

    val username = user.username
    val email = user.email
    val parameters = mapOf(
      "firstName" to user.firstName,
      "fullName" to user.firstName,
      "resetLink" to passwordLink
    )

    // send the email
    try {
      log.info("Sending initial set password to notify for user {}", username)
      notificationClient.sendEmail(initialPasswordTemplateId, email, parameters, null)
      telemetryClient.trackEvent("${eventPrefix}Success", mapOf("username" to username), null)
    } catch (e: NotificationClientException) {
      val reason = (e.cause?.let { e.cause } ?: e).javaClass.simpleName
      log.warn("Failed to send create user notify for user {}", username, e)
      telemetryClient.trackEvent(
        "${eventPrefix}Failure",
        mapOf("username" to username, "reason" to reason),
        null
      )
      if (e.httpResult >= 500) { // second time lucky
        notificationClient.sendEmail(initialPasswordTemplateId, email, parameters, null, null)
        telemetryClient.trackEvent("${eventPrefix}Success", mapOf("username" to username), null)
      }
      throw e
    }
  }

  private fun getPasswordResetLink(token: String): String {
    return "$authBaseUri/initial-password?token=$token"
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

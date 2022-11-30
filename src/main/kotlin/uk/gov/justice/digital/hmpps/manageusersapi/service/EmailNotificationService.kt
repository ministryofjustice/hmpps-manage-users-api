package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException

@Service
class EmailNotificationService(
  private val notificationClient: NotificationClientApi,
  private val telemetryClient: TelemetryClient,
  @Value("\${api.base.url.oauth}") private val authBaseUri: String,
  @Value("\${application.notify.enable-user.template}") private val enableUserTemplateId: String,
) {

  fun sendEnableEmail(emailNotificationDto: EmailNotificationDto) {
    with(emailNotificationDto) {
      val parameters = mapOf(
        "firstName" to firstName,
        "username" to username,
        "signinUrl" to authBaseUri,
      )
      // send the email
      try {
        log.info("Sending enable user email to notify for user {}", username)
        notificationClient.sendEmail(enableUserTemplateId, email, parameters, null)
      } catch (e: NotificationClientException) {
        val reason = (e.cause?.let { e.cause } ?: e).javaClass.simpleName
        log.warn("Failed to send enable user email for user {}", username, e)
        telemetryClient.trackEvent(
          "ExternalUserEnabledEmailFailure",
          mapOf("username" to username, "reason" to reason, "admin" to admin),
          null
        )
        throw e
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

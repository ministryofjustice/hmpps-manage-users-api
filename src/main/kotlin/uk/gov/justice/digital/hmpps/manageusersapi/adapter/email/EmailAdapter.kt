package uk.gov.justice.digital.hmpps.manageusersapi.adapter.email

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException

@Service
class EmailAdapter(
  private val notificationClient: NotificationClientApi,
  private val telemetryClient: TelemetryClient,
  private val authenticationFacade: AuthenticationFacade,
) {

  fun send(templateId: String, personalisation: Map<String, Any>, eventPrefix: String, username: String, email: String) {
    try {
      log.info("Sending $eventPrefix to notify for user {}", username)
      notificationClient.sendEmail(templateId, email, personalisation, null)
    } catch (e: NotificationClientException) {
      val reason = (e.cause?.let { e.cause } ?: e).javaClass.simpleName
      log.warn("Failed to send $eventPrefix for user {}", username, e)
      telemetryClient.trackEvent(
        "${eventPrefix}Failure",
        mapOf("username" to username, "reason" to reason, "admin" to authenticationFacade.currentUsername),
        null
      )
      throw e
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

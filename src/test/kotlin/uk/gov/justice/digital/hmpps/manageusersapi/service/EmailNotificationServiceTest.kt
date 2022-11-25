package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.EmailNotificationDto
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException

class EmailNotificationServiceTest {
  private val notificationClient: NotificationClientApi = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val emailNotificationService =
    EmailNotificationService(notificationClient, telemetryClient, "http://localhost:9090/auth", "template-id")

  @Nested
  inner class CreateToken {
    @Test
    fun `Create token and send email notification`() {
      val emailNotificationDto = EmailNotificationDto("CEN_ADM", "firstName", "cadmin@gov.uk", "admin")
      val parameters = mapOf(
        "firstName" to emailNotificationDto.firstName,
        "username" to emailNotificationDto.username,
        "signinUrl" to "http://localhost:9090/auth"
      )
      emailNotificationService.sendEnableEmail(emailNotificationDto)
      verify(notificationClient).sendEmail("template-id", emailNotificationDto.email, parameters, null)
    }

    @Test
    fun `Send Failure Telemetry Event`() {

      val emailNotificationDto = EmailNotificationDto("CEN_ADM", "firstName", "cadmin@gov.uk", "admin")
      val parameters = mapOf(
        "firstName" to emailNotificationDto.firstName,
        "username" to emailNotificationDto.username,
        "signinUrl" to "http://localhost:9090/auth"
      )
      whenever(notificationClient.sendEmail("template-id", emailNotificationDto.email, parameters, null)).thenThrow(
        NotificationClientException(
          "USER_DUP",
        )
      )

      Assertions.assertThatExceptionOfType(NotificationClientException::class.java)
        .isThrownBy { emailNotificationService.sendEnableEmail(emailNotificationDto) }
      verify(telemetryClient).trackEvent(
        "ExternalUserEnabledEmailFailure",
        mapOf("username" to emailNotificationDto.username, "reason" to "NotificationClientException", "admin" to emailNotificationDto.admin),
        null
      )
    }
  }
}

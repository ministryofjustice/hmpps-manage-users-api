package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException

class TokenServiceTest {
  private val notificationClient: NotificationClientApi = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authService: AuthService = mock()
  private val tokenService =
    TokenService(notificationClient, telemetryClient, authService, "http://localhost:9090/auth", "template-id")

  @Nested
  inner class CreateToken {
    @Test
    fun `Create token and send email notification`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)
      val parameters = mapOf(
        "firstName" to user.firstName,
        "fullName" to user.firstName,
        "resetLink" to "http://localhost:9090/auth/initial-password?token=new-token"
      )
      whenever(authService.createNewToken(any())).thenReturn("new-token")

      tokenService.saveAndSendInitialEmail(user, "DPSUserCreate")

      verify(telemetryClient).trackEvent("DPSUserCreateSuccess", mapOf("username" to user.username), null)
      verify(notificationClient).sendEmail("template-id", user.email, parameters, null)
    }

    @Test
    fun `Send Failure Telemetry Event`() {
      val user = CreateUserRequest("USER_DUP", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)
      val parameters = mapOf(
        "firstName" to user.firstName,
        "fullName" to user.firstName,
        "resetLink" to "http://localhost:9090/auth/initial-password?token=new-token"
      )
      whenever(notificationClient.sendEmail("template-id", user.email, parameters, null)).thenThrow(
        NotificationClientException(
          "USER_DUP",
        )
      )
      whenever(authService.createNewToken(any())).thenReturn("new-token")

      assertThatExceptionOfType(NotificationClientException::class.java)
        .isThrownBy { tokenService.saveAndSendInitialEmail(user, "DPSUserCreate") }
      verify(telemetryClient).trackEvent(
        "DPSUserCreateFailure",
        mapOf("username" to user.username, "reason" to "NotificationClientException"),
        null
      )
    }
  }
}

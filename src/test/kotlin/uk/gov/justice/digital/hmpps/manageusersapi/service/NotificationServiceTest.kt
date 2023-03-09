package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.EmailNotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType
import uk.gov.service.notify.NotificationClientException

class NotificationServiceTest {
  private val emailNotificationService: EmailNotificationService = mock()
  private val authService: AuthApiService = mock()
  private val notificationService =
    NotificationService(emailNotificationService, authService, "http://localhost:9090/auth", "template-id")

  @Nested
  inner class NewPrisonUserNotification {
    @Test
    fun `Create token and send email notification`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)
      val parameters = mapOf(
        "firstName" to user.firstName,
        "fullName" to user.firstName,
        "resetLink" to "http://localhost:9090/auth/initial-password?token=new-token"
      )
      whenever(authService.createNewToken(any())).thenReturn("new-token")

      notificationService.newPrisonUserNotification(user, "DPSUserCreate")

      verify(emailNotificationService).send("template-id", parameters, "DPSUserCreate", user.username, user.email)
    }

    @Test
    fun `Throws exception thrown by email adapter`() {
      val user = CreateUserRequest("USER_DUP", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)
      val parameters = mapOf(
        "firstName" to user.firstName,
        "fullName" to user.firstName,
        "resetLink" to "http://localhost:9090/auth/initial-password?token=new-token"
      )

      doAnswer {
        throw NotificationClientException("USER_DUP")
      }.whenever(emailNotificationService).send("template-id", parameters, "DPSUserCreate", user.username, user.email)

      whenever(authService.createNewToken(any())).thenReturn("new-token")

      assertThatExceptionOfType(NotificationClientException::class.java)
        .isThrownBy { notificationService.newPrisonUserNotification(user, "DPSUserCreate") }
    }
  }
}

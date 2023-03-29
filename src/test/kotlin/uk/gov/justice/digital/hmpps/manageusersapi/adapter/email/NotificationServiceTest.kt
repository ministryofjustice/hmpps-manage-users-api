package uk.gov.justice.digital.hmpps.manageusersapi.adapter.email

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.TokenByEmailTypeRequest
import uk.gov.justice.digital.hmpps.manageusersapi.model.EnabledExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.EmailType
import uk.gov.service.notify.NotificationClientException
import java.util.UUID

class NotificationServiceTest {
  private val emailAdapter: EmailAdapter = mock()
  private val authService: AuthApiService = mock()

  private val initialPasswordTemplateId = "initial-password-template-id"
  private val enableUserTemplateId = "enable-user-template-id"
  private val notifyTemplateId = "notify-template-id"
  private val authBaseUri = "http://localhost:9090/auth"

  private val notificationService =
    NotificationService(
      emailAdapter,
      authService,
      authBaseUri,
      initialPasswordTemplateId,
      enableUserTemplateId,
      notifyTemplateId,
    )

  @Nested
  inner class NewPrisonUserNotification {
    @Test
    fun `Creates token and sends email notification`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)
      val parameters = mapOf(
        "firstName" to user.firstName,
        "fullName" to user.firstName,
        "resetLink" to "http://localhost:9090/auth/initial-password?token=new-token",
      )
      whenever(authService.createNewToken(any())).thenReturn("new-token")

      notificationService.newPrisonUserNotification(user, "DPSUserCreate")

      verify(emailAdapter).send(initialPasswordTemplateId, parameters, "DPSUserCreate", user.username, user.email)
    }

    @Test
    fun `Throws exception thrown by email adapter`() {
      val user = CreateUserRequest("USER_DUP", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)
      val parameters = mapOf(
        "firstName" to user.firstName,
        "fullName" to user.firstName,
        "resetLink" to "http://localhost:9090/auth/initial-password?token=new-token",
      )

      doAnswer {
        throw NotificationClientException("USER_DUP")
      }.whenever(emailAdapter).send(initialPasswordTemplateId, parameters, "DPSUserCreate", user.username, user.email)

      whenever(authService.createNewToken(any())).thenReturn("new-token")

      Assertions.assertThatExceptionOfType(NotificationClientException::class.java)
        .isThrownBy { notificationService.newPrisonUserNotification(user, "DPSUserCreate") }
    }
  }

  @Nested
  inner class ExternalUserEnabledNotification {
    @Test
    fun `Does not send email when email address null`() {
      val user = EnabledExternalUser(username = "testy", firstName = "testing", email = null, admin = "admin")

      notificationService.externalUserEnabledNotification(user)

      verifyNoInteractions(emailAdapter)
    }

    @Test
    fun `Sends email when email address present`() {
      val user =
        EnabledExternalUser(username = "testy", firstName = "testing", email = "testy@testing.com", admin = "admin")

      notificationService.externalUserEnabledNotification(user)

      val expectedParameters = mapOf(
        "firstName" to "testing",
        "username" to "testy",
        "signinUrl" to authBaseUri,
      )

      verify(emailAdapter).send(enableUserTemplateId, expectedParameters, "ExternalUserEnabledEmail", user.username, user.email!!)
    }

    @Test
    fun `Throws exception thrown by email adapter`() {
      val user =
        EnabledExternalUser(username = "testy", firstName = "testing", email = "testy@testing.com", admin = "admin")

      val expectedParameters = mapOf(
        "firstName" to "testing",
        "username" to "testy",
        "signinUrl" to authBaseUri,
      )

      doAnswer {
        throw NotificationClientException("USER_DUP")
      }.whenever(emailAdapter).send(enableUserTemplateId, expectedParameters, "ExternalUserEnabledEmail", user.username, user.email!!)

      Assertions.assertThatExceptionOfType(NotificationClientException::class.java)
        .isThrownBy { notificationService.externalUserEnabledNotification(user) }
    }
  }

  @Nested
  inner class ExternalUserEmailAmendInitialNotification {

    private val userId: UUID = UUID.randomUUID()

    @Test
    fun `Creates reset token and sends email notification`() {
      val user = buildExternalUser(userId)
      whenever(authService.createResetTokenForUser(userId)).thenReturn("reset-token")

      notificationService.externalUserEmailAmendInitialNotification(userId, user, "new.testy@testing.com", "newtesty", "support-link")

      val expectedName = "${user.firstName} ${user.lastName}"
      val expectedParameters = mapOf(
        "firstName" to expectedName,
        "fullName" to expectedName,
        "resetLink" to "$authBaseUri/initial-password?token=reset-token",
        "supportLink" to "support-link",
      )

      verify(emailAdapter).send(initialPasswordTemplateId, expectedParameters, "AuthUserAmend", "newtesty", "new.testy@testing.com")
    }

    @Test
    fun `Throws exception thrown by email adapter`() {
      val user = buildExternalUser(userId)
      whenever(authService.createResetTokenForUser(userId)).thenReturn("reset-token")

      val expectedName = "${user.firstName} ${user.lastName}"
      val expectedParameters = mapOf(
        "firstName" to expectedName,
        "fullName" to expectedName,
        "resetLink" to "$authBaseUri/initial-password?token=reset-token",
        "supportLink" to "support-link",
      )

      doAnswer {
        throw NotificationClientException("USER_DUP")
      }.whenever(emailAdapter).send(initialPasswordTemplateId, expectedParameters, "AuthUserAmend", "newtesty", "new.testy@testing.com")

      Assertions.assertThatExceptionOfType(NotificationClientException::class.java)
        .isThrownBy { notificationService.externalUserEmailAmendInitialNotification(userId, user, "new.testy@testing.com", "newtesty", "support-link") }
    }
  }

  @Nested
  inner class VerifyEmailNotification {
    private val userId: UUID = UUID.randomUUID()

    @Test
    fun `Creates token by email type and sends email`() {
      val user = buildExternalUser(userId)
      whenever(authService.createTokenByEmailType(TokenByEmailTypeRequest(user.username, EmailType.PRIMARY.name))).thenReturn("token-by-email")

      notificationService.verifyEmailNotification(user, "testy@testing.com")

      val expectedParameters = mapOf(
        "firstName" to user.firstName,
        "fullName" to "${user.firstName} ${user.lastName}",
        "verifyLink" to "$authBaseUri/verify-email-confirm?token=token-by-email",
      )
      verify(emailAdapter).send(notifyTemplateId, expectedParameters, "VerifyEmailRequest", user.username, "testy@testing.com")
    }

    @Test
    fun `Throws exception thrown by email adapter`() {
      val user = buildExternalUser(userId)
      whenever(authService.createTokenByEmailType(TokenByEmailTypeRequest(user.username, EmailType.PRIMARY.name))).thenReturn("token-by-email")

      val expectedParameters = mapOf(
        "firstName" to user.firstName,
        "fullName" to "${user.firstName} ${user.lastName}",
        "verifyLink" to "$authBaseUri/verify-email-confirm?token=token-by-email",
      )

      doAnswer {
        throw NotificationClientException("USER_DUP")
      }.whenever(emailAdapter).send(notifyTemplateId, expectedParameters, "VerifyEmailRequest", user.username, "testy@testing.com")

      Assertions.assertThatExceptionOfType(NotificationClientException::class.java)
        .isThrownBy { notificationService.verifyEmailNotification(user, "testy@testing.com") }
    }
  }

  private fun buildExternalUser(userId: UUID): ExternalUser {
    return ExternalUser(
      userId = userId,
      "testy",
      "testy@testing.com",
      "testy",
      "McTest",
      locked = false,
      enabled = true,
      verified = true,
    )
  }
}

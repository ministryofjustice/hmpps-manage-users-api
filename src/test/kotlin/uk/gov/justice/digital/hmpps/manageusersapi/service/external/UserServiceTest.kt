package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationDetails
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserGroupApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import java.util.UUID

class UserServiceTest {
  private val notificationService: NotificationService = mock()
  private val userApiService: UserApiService = mock()
  private val externalUsersSearchApiService: UserSearchApiService = mock()
  private val authApiService: AuthApiService = mock()
  private val userGroupApiService: UserGroupApiService = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val authBaseUri: String = "test-auth-base-uri"
  private val initialPasswordTemplateId: String = "test-initial-password-template"
  private val enableUserTemplateId: String = "test-enable-user-template"

  private val userService = UserService(
    notificationService,
    userApiService,
    externalUsersSearchApiService,
    authApiService,
    userGroupApiService,
    verifyEmailService,
    telemetryClient,
    authBaseUri,
    initialPasswordTemplateId,
    enableUserTemplateId
  )
  private val userUUID: UUID = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")

  @Nested
  inner class EnableExternalUser {

    @Test
    fun `enable user by userId sends email`() {
      val emailNotificationDto = EmailNotificationDto("CEN_ADM", "firstName", "cadmin@gov.uk", "admin")
      whenever(userApiService.enableUserById(anyOrNull())).thenReturn(emailNotificationDto)

      with(emailNotificationDto) {
        val expectedParameters = mapOf(
          "firstName" to firstName,
          "username" to username,
          "signinUrl" to authBaseUri,
        )

        userService.enableUserByUserId(userUUID)

        verify(notificationService).send(
          eq(enableUserTemplateId), eq(expectedParameters), eq("ExternalUserEnabledEmail"),
          eq(
            NotificationDetails(username, email!!)
          )
        )
      }
    }

    @Test
    fun `enable user by userId doesn't sends notification email`() {
      val emailNotificationDto = EmailNotificationDto("CEN_ADM", "firstName", null, "admin")
      whenever(userApiService.enableUserById(anyOrNull())).thenReturn(emailNotificationDto)
      userService.enableUserByUserId(userUUID)
      verifyNoInteractions(notificationService)
    }
  }

  @Nested
  inner class DisableExternalUser {

    @Test
    fun `disable user by userId sends email`() {
      val reason = DeactivateReason("Fired")
      userService.disableUserByUserId(userUUID, reason)
      verify(userApiService).disableUserById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), reason)
    }
  }
}

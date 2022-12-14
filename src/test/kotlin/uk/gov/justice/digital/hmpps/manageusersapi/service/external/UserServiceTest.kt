package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailNotificationService
import java.util.UUID

class ExternalUserServiceTest {
  private val externalUsersApiService: ExternalUsersApiService = mock()
  private val emailNotificationService: EmailNotificationService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val userService = UserService(
    externalUsersApiService,
    emailNotificationService,
    telemetryClient
  )
  private val userUUID: UUID = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")

  @Nested
  inner class EnableExternalUser {

    @Test
    fun `enable user by userId sends email`() {
      val emailNotificationDto = EmailNotificationDto("CEN_ADM", "firstName", "cadmin@gov.uk", "admin")
      whenever(externalUsersApiService.enableUserById(anyOrNull())).thenReturn(emailNotificationDto)
      userService.enableUserByUserId(userUUID)
      verify(emailNotificationService).sendEnableEmail(emailNotificationDto)
    }

    @Test
    fun `enable user by userId doesn't sends notification email`() {
      val emailNotificationDto = EmailNotificationDto("CEN_ADM", "firstName", null, "admin")
      whenever(externalUsersApiService.enableUserById(anyOrNull())).thenReturn(emailNotificationDto)
      userService.enableUserByUserId(userUUID)
      verifyNoInteractions(emailNotificationService)
    }
  }

  @Nested
  inner class DisableExternalUser {

    @Test
    fun `disable user by userId sends email`() {
      val reason = DeactivateReason("Fired")
      userService.disableUserByUserId(userUUID, reason)
      verify(externalUsersApiService).disableUserById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), reason)
    }
  }
}

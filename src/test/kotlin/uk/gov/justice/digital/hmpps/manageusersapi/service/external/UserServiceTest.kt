package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailNotificationService
import java.util.UUID

class UserServiceTest {
  private val userApiService: UserApiService = mock()
  private val emailNotificationService: EmailNotificationService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()

  private val userService = UserService(
    userApiService,
    emailNotificationService,
    telemetryClient,
    authenticationFacade,
  )
  private val userUUID: UUID = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")

  @Nested
  inner class EnableExternalUser {

    @Test
    fun `enable user by userId sends email`() {
      val emailNotificationDto = EmailNotificationDto("CEN_ADM", "firstName", "cadmin@gov.uk", "admin")
      whenever(userApiService.enableUserById(anyOrNull())).thenReturn(emailNotificationDto)
      userService.enableUserByUserId(userUUID)
      verify(emailNotificationService).sendEnableEmail(emailNotificationDto)
    }

    @Test
    fun `enable user by userId doesn't sends notification email`() {
      val emailNotificationDto = EmailNotificationDto("CEN_ADM", "firstName", null, "admin")
      whenever(userApiService.enableUserById(anyOrNull())).thenReturn(emailNotificationDto)
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
      verify(userApiService).disableUserById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), reason)
    }
  }

  @Nested
  inner class MyRoles {
    @Test
    fun myRoles(): Unit = runBlocking {
      whenever(authenticationFacade.authentication).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(listOf(SimpleGrantedAuthority("ROLE_BOB"), SimpleGrantedAuthority("ROLE_JOE_FRED")))
      assertThat(userService.myRoles()).containsOnly(ExternalUserRole("BOB"), ExternalUserRole("JOE_FRED"))
    }

    @Test
    fun myRoles_noRoles(): Unit = runBlocking {
      whenever(authenticationFacade.authentication).thenReturn(authentication)
      whenever(authentication.authorities).thenReturn(emptyList())
      assertThat(userService.myRoles()).isEmpty()
    }
  }
}

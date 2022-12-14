package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.ExternalUsersApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.NomisApiService
import java.util.UUID

class UserServiceTest {
  private val nomisService: NomisApiService = mock()
  private val tokenService: TokenService = mock()
  private val verifyEmailDomainService: VerifyEmailDomainService = mock()
  private val externalUsersApiService: ExternalUsersApiService = mock()
  private val emailNotificationService: EmailNotificationService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val userService = UserService(
    nomisService,
    tokenService,
    verifyEmailDomainService,
    externalUsersApiService,
    emailNotificationService,
    telemetryClient
  )
  private val userUUID: UUID = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")

  @Nested
  inner class CreateUser {
    @Test
    fun `create a DPS central admin user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)

      whenever(nomisService.createCentralAdminUser(user)).thenReturn(
        NomisUserDetails(
          user.username,
          user.email,
          user.firstName,
          user.lastName,
        )
      )
      userService.createUser(user)
      verify(nomisService).createCentralAdminUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS general user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_GEN, "MDI")
      whenever(nomisService.createGeneralUser(user)).thenReturn(
        NomisUserDetails(
          user.username,
          user.email,
          user.firstName,
          user.lastName,
        )
      )
      userService.createUser(user)
      verify(nomisService).createGeneralUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS local admin user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_LSA, "MDI")
      whenever(nomisService.createLocalAdminUser(user)).thenReturn(
        NomisUserDetails(
          user.username,
          user.email,
          user.firstName,
          user.lastName,
        )
      )
      userService.createUser(user)
      verify(nomisService).createLocalAdminUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `should validate email domain`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(false)
      val userWithInvalidEmailDomain = CreateUserRequest(
        "CEN_ADM",
        "cadmin@test.gov.uk",
        "First",
        "Last",
        UserType.DPS_LSA,
        "MDI"
      )

      assertThatThrownBy { userService.createUser(userWithInvalidEmailDomain) }
        .isInstanceOf(HmppsValidationException::class.java)
        .hasMessage("Invalid Email domain: test.gov.uk with reason: Email domain not valid")
    }
  }

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

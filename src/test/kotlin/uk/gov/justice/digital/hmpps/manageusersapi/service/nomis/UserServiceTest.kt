package uk.gov.justice.digital.hmpps.manageusersapi.service.nomis

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType
import uk.gov.justice.digital.hmpps.manageusersapi.service.TokenService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService

class UserServiceTest {
  private val nomisUserCreateService: UserApiService = mock()
  private val tokenService: TokenService = mock()
  private val verifyEmailDomainService: VerifyEmailDomainService = mock()
  private val nomisUserService = UserService(
    nomisUserCreateService,
    tokenService,
    verifyEmailDomainService,
  )

  @Nested
  inner class CreateUser {
    @Test
    fun `create a DPS central admin user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)

      whenever(nomisUserCreateService.createCentralAdminUser(user)).thenReturn(
        NomisUserDetails(
          user.username,
          user.email,
          user.firstName,
          user.lastName,
        )
      )
      nomisUserService.createUser(user)
      verify(nomisUserCreateService).createCentralAdminUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS general user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_GEN, "MDI")
      whenever(nomisUserCreateService.createGeneralUser(user)).thenReturn(
        NomisUserDetails(
          user.username,
          user.email,
          user.firstName,
          user.lastName,
        )
      )
      nomisUserService.createUser(user)
      verify(nomisUserCreateService).createGeneralUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS local admin user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_LSA, "MDI")
      whenever(nomisUserCreateService.createLocalAdminUser(user)).thenReturn(
        NomisUserDetails(
          user.username,
          user.email,
          user.firstName,
          user.lastName,
        )
      )
      nomisUserService.createUser(user)
      verify(nomisUserCreateService).createLocalAdminUser(user)
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

      assertThatThrownBy { nomisUserService.createUser(userWithInvalidEmailDomain) }
        .isInstanceOf(HmppsValidationException::class.java)
        .hasMessage("Invalid Email domain: test.gov.uk with reason: Email domain not valid")
    }
  }
}

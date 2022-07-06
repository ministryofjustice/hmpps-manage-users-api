package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType

class UserServiceTest {
  private val nomisService: NomisApiService = mock()
  private val tokenService: TokenService = mock()
  private val userService = UserService(nomisService, tokenService)

  @Nested
  inner class CreateUser {
    @Test
    fun `create a DPS central admin user`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)

      whenever(nomisService.createCentralAdminUser(user)).thenReturn(
        NomisUserDetails(
          user.username,
          user.email,
          user.firstName,
          user.lastName
        )
      )
      userService.createUser(user)
      verify(nomisService).createCentralAdminUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS general user`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_GEN, "MDI")
      whenever(nomisService.createGeneralUser(user)).thenReturn(
        NomisUserDetails(
          user.username,
          user.email,
          user.firstName,
          user.lastName
        )
      )
      userService.createUser(user)
      verify(nomisService).createGeneralUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS local admin user`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_LSA, "MDI")
      whenever(nomisService.createLocalAdminUser(user)).thenReturn(
        NomisUserDetails(
          user.username,
          user.email,
          user.firstName,
          user.lastName
        )
      )
      userService.createUser(user)
      verify(nomisService).createLocalAdminUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }
  }
}

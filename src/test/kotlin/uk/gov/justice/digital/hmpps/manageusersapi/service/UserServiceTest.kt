package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType

class UserServiceTest {
  private val nomisService: NomisApiService = mock()
  private val userService = UserService(nomisService)

  @Nested
  inner class CreateUser {
    @Test
    fun `create a DPS central admin user`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)

      userService.createUser(user)
      verify(nomisService).createCentralAdminUser(user)
    }

    @Test
    fun `create a DPS general user`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_GEN, "MDI")

      userService.createUser(user)
      verify(nomisService).createGeneralUser(user)
    }

    @Test
    fun `create a DPS local admin user`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_LSA, "MDI")

      userService.createUser(user)
      verify(nomisService).createLocalAdminUser(user)
    }
  }
}

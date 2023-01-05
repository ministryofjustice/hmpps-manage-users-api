package uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.UserService

class UserControllerTest {

  private val userService: UserService = mock()
  private val userController = UserController(userService)

  @Nested
  inner class CreateUser {
    @Test
    fun `create DPS user`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)
      userController.createUser(user)
      verify(userService).createUser(user)
    }
  }
}

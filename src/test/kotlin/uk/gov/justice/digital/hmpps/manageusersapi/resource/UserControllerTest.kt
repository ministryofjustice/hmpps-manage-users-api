package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserExistsException
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserSearchService
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserService

class UserControllerTest {

  private val userService: UserService = mock()
  private val userSearchService: UserSearchService = mock()
  private val userController = UserController(userService, userSearchService)

  @Nested
  inner class CreateUser {
    @Test
    fun `create DPS user`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)
      userController.createUser(user)
      verify(userService).createUser(user)
    }

    @Test
    fun `create user that already exists throws exception`() {
      whenever(userService.createUser(any())).thenThrow(
        UserExistsException(
          "USER_DUP",
          "user name already exists"
        )
      )
      val user = CreateUserRequest("USER_DUP", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)

      assertThatThrownBy { userController.createUser(user) }
        .isInstanceOf(UserExistsException::class.java)
        .withFailMessage("Unable to create user: USER_DUP with reason: user name already exists")
    }
  }

  @Nested
  inner class FindVerifiedEmailsOfUsers {
    @Test
    fun `find verified user`() {
      val userNames = listOf("CEN_ADM", "CEN_ADM1")
      userController.getUsersEmails(userNames)
      verify(userSearchService).findUsersByUsernames(userNames)
    }
  }
}

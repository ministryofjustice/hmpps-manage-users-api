package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.auth
import uk.gov.justice.digital.hmpps.manageusersapi.model.GenericUser
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserService
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.NotFoundException
import java.util.UUID

class UserControllerTest {

  private val userService: UserService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val userController = UserController(userService, authenticationFacade)

  @Test
  fun `find user by username`() {
    val username = "AUTH_ADM"
    val userDetails = GenericUser(
      username,
      true,
      "Any User",
      auth,
      userId = UUID.randomUUID().toString(),
      uuid = UUID.randomUUID()
    )
    whenever(userService.findUserByUsername(username)).thenReturn(userDetails)

    val user = userController.findUser(username)
    verify(userService).findUserByUsername(username)
    assertThat(user).isEqualTo(UserDetailsDto.fromDomain(userDetails))
  }

  @Test
  fun `find user by username should throw exception when username not found`() {
    whenever(userService.findUserByUsername("username")).thenReturn(null)

    assertThatThrownBy {
      userController.findUser("username")
    }.isInstanceOf(NotFoundException::class.java)
  }

  @Test
  fun `find my details`() {
    val userDetails = GenericUser(
      "username",
      true,
      "Any User",
      auth,
      userId = UUID.randomUUID().toString(),
      uuid = UUID.randomUUID()
    )
    whenever(authenticationFacade.currentUsername).thenReturn("me")
    whenever(userService.findUserByUsername("me")).thenReturn(userDetails)

    val user = userController.myDetails()
    verify(userService).findUserByUsername("me")
    assertThat(user).isEqualTo(UserDetailsDto.fromDomain(userDetails))
  }

  @Test
  fun `find my details for basic user`() {
    whenever(authenticationFacade.currentUsername).thenReturn("me")
    val userDetails = UsernameDto("me")
    whenever(userService.findUserByUsername("me")).thenReturn(null)

    val user = userController.myDetails()

    verify(userService).findUserByUsername("me")
    assertThat(user).isEqualTo(userDetails)
  }

  @Nested
  inner class UserRoles {
    @Test
    fun userRoles_extUser() {
      whenever(userService.findRolesByUsername(ArgumentMatchers.anyString())).thenReturn(
        listOf(
          UserRole(
            roleCode = "TEST_ROLE"
          )
        )
      )
      assertThat(userController.userRoles("JOE")).contains(UserRole(roleCode = "TEST_ROLE"))
    }
    @Test
    fun userRoles_notFound() {
      whenever(userService.findRolesByUsername(ArgumentMatchers.anyString())).thenReturn(null)
      assertThatThrownBy { userController.userRoles("JOE") }
        .isInstanceOf(NotFoundException::class.java)
        .hasMessage("Account for username JOE not found")
    }
  }
}

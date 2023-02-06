package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserService
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.NotFoundException
import java.util.UUID

class UserControllerTest {

  private val userService: UserService = mock()
  private val userController = UserController(userService)

  @Test
  fun `find user by username`() {
    val username = "AUTH_ADM"
    val userDetails = UserDetailsDto(
      username,
      true,
      "External User",
      AuthSource.azuread,
      UUID.randomUUID().toString(),
      UUID.randomUUID()
    )
    whenever(userService.findUserByUsername(username)).thenReturn(userDetails)

    val user = userController.findUser(username)
    verify(userService).findUserByUsername(username)
    assertThat(user).isEqualTo(userDetails)
  }

  @Test
  fun `should throw exception when username not found`() {
    whenever(userService.findUserByUsername("username")).thenReturn(null)

    assertThatThrownBy {
      userController.findUser("username")
    }.isInstanceOf(NotFoundException::class.java)
  }
}

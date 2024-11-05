package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.auth
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailAddress
import uk.gov.justice.digital.hmpps.manageusersapi.model.GenericUser
import uk.gov.justice.digital.hmpps.manageusersapi.service.ExternalUserRole
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserService
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.NotFoundException
import java.util.UUID

class UserControllerTest {

  private val userService: UserService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val myRolesEndpointIsEnabled = true
  private var userController = UserController(userService, authenticationFacade, myRolesEndpointIsEnabled)

  @Test
  fun `find user by username`() {
    val username = "AUTH_ADM"
    val userDetails = GenericUser(
      username,
      true,
      "Any User",
      auth,
      userId = UUID.randomUUID().toString(),
      uuid = UUID.randomUUID(),
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
      uuid = UUID.randomUUID(),
    )
    whenever(authenticationFacade.currentUsername).thenReturn("me")
    whenever(userService.findUserByUsernameWithAuthSource("me")).thenReturn(userDetails)

    val user = userController.myDetails()
    verify(userService).findUserByUsernameWithAuthSource("me")
    assertThat(user).isEqualTo(UserDetailsDto.fromDomain(userDetails))
  }

  @Test
  fun `find my details for basic user`() {
    whenever(authenticationFacade.currentUsername).thenReturn("me")
    val userDetails = UsernameDto("me")
    whenever(userService.findUserByUsernameWithAuthSource("me")).thenReturn(null)

    val user = userController.myDetails()

    verify(userService).findUserByUsernameWithAuthSource("me")
    assertThat(user).isEqualTo(userDetails)
  }

  @Nested
  inner class UserRoles {
    @Test
    fun userRoles_extUser() {
      whenever(userService.findRolesByUsername(ArgumentMatchers.anyString())).thenReturn(
        listOf(
          UserRole(
            roleCode = "TEST_ROLE",
          ),
        ),
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

  @Nested
  inner class UserEmail {
    @Test
    fun userEmail_found() {
      whenever(userService.findUserEmail(any(), any())).thenReturn(
        EmailAddress(
          username = "JOE",
          verified = true,
          email = "someemail",
        ),
      )

      val responseEntity = userController.getUserEmail("joe")
      assertThat(responseEntity.statusCodeValue).isEqualTo(200)
      assertThat(responseEntity.body).usingRecursiveComparison().isEqualTo(EmailAddress("JOE", "someemail", true))
    }

    @Test
    fun userEmail_found_unverified() {
      whenever(userService.findUserEmail(any(), any())).thenReturn(
        EmailAddress(username = "JOE", verified = false, email = "someemail"),
      )
      val responseEntity = userController.getUserEmail("joe", unverified = true)
      assertThat(responseEntity.statusCodeValue).isEqualTo(200)
      assertThat(responseEntity.body).usingRecursiveComparison().isEqualTo(EmailAddress("JOE", "someemail", false))
    }

    @Test
    fun userEmail_notFound() {
      whenever(userService.findUserEmail(any(), any())).thenReturn(null)

      assertThatThrownBy { userController.getUserEmail("joe") }
        .isInstanceOf(NotFoundException::class.java)
        .hasMessage("Account for username joe not found")
    }

    @Test
    fun userEmail_notVerified() {
      whenever(userService.findUserEmail(any(), any())).thenReturn(
        EmailAddress(username = "JOE", verified = false, email = null),
      )
      val responseEntity = userController.getUserEmail("joe")
      assertThat(responseEntity.statusCodeValue).isEqualTo(204)
      assertThat(responseEntity.body).isNull()
    }

    @Test
    fun userEmail_null() {
      whenever(userService.findUserEmail(any(), any())).thenReturn(
        EmailAddress(username = "JOE", verified = false, email = null),
      )
      val responseEntity = userController.getUserEmail("joe", unverified = true)
      assertThat(responseEntity.statusCodeValue).isEqualTo(200)
      assertThat(responseEntity.body).usingRecursiveComparison().isEqualTo(EmailAddress("JOE", null, false))
    }
  }

  @Nested
  inner class MyEmail {
    @Test
    fun myEmail() {
      whenever(authenticationFacade.currentUsername).thenReturn("me")
      whenever(userService.findUserEmail(any(), any())).thenReturn(
        EmailAddress(
          username = "JOE",
          verified = true,
          email = "someemail",
        ),
      )
      val responseEntity = userController.myEmail()
      assertThat(responseEntity.statusCodeValue).isEqualTo(200)
      assertThat(responseEntity.body).usingRecursiveComparison().isEqualTo(EmailAddress("JOE", "someemail", true))
    }
  }

  @Nested
  inner class MyRoles {
    @Test
    fun myRolesWhenEndpointIsEnabled() {
      val externalUserRoles = listOf(ExternalUserRole("ROLE_SOMETHING"))
      whenever(userService.myRoles()).thenReturn(externalUserRoles)

      val responseEntity = userController.myRoles()

      assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(responseEntity.body).usingRecursiveComparison().isEqualTo(externalUserRoles)
    }
    @Test
    fun myRolesWhenEndpointIsDisabled() {
      userController = UserController(userService, authenticationFacade, false)

      val responseEntity = userController.myRoles()

      assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.GONE)
      assertThat(responseEntity.body).usingRecursiveComparison().isEqualTo("This endpoint is deprecated and will be removed soon. Use /auth/api/user/me instead.")
    }
  }
}

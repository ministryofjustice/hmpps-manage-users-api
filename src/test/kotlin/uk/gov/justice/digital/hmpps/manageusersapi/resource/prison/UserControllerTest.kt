package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.EnhancedPrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserService

class UserControllerTest {

  private val userService: UserService = mock()
  private val userController = UserController(userService, false)

  @Nested
  inner class CreateUser {
    @Test
    fun `create DPS user`() {
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)
      whenever(userService.createUser(user)).thenReturn(createPrisonUserDetails())

      assertThat(userController.createUser(user)).isEqualTo(
        NewPrisonUserDto(
          username = "NUSER_GEN",
          primaryEmail = "nomis.usergen@digital.justice.gov.uk",
          firstName = "Nomis",
          lastName = "Take",
        ),
      )
      verify(userService).createUser(user)
    }
  }

  @Nested
  inner class CreateLinkedCentralAdminUser {
    @Test
    fun `create Linked DPS Central Admin user`() {
      val createLinkedCentralAdminUserRequest = CreateLinkedCentralAdminUserRequest("TEST_USER", "TEST_USER_ADM")
      whenever(userService.createLinkedCentralAdminUser(createLinkedCentralAdminUserRequest)).thenReturn(
        UserFixture.createPrisonStaffUser(),
      )
      userController.createLinkedCentralAdminUser(createLinkedCentralAdminUserRequest)
      verify(userService).createLinkedCentralAdminUser(createLinkedCentralAdminUserRequest)
    }
  }

  @Nested
  inner class CreateLinkedLocalAdminUser {
    @Test
    fun `create Linked DPS Local Admin user`() {
      val createLinkedLocalAdminUserRequest = CreateLinkedLocalAdminUserRequest("TEST_USER", "TEST_USER_ADM", "MDI")
      whenever(userService.createLinkedLocalAdminUser(createLinkedLocalAdminUserRequest)).thenReturn(
        UserFixture.createPrisonStaffUser(),
      )
      userController.createLinkedLocalAdminUser(createLinkedLocalAdminUserRequest)
      verify(userService).createLinkedLocalAdminUser(createLinkedLocalAdminUserRequest)
    }
  }

  @Nested
  inner class CreateLinkedGeneralUser {
    @Test
    fun `create Linked General user`() {
      val createLinkedGeneralUserRequest = CreateLinkedGeneralUserRequest("TEST_USER_ADM", "TEST_USER_GEN", "BXI")
      whenever(userService.createLinkedGeneralUser(createLinkedGeneralUserRequest)).thenReturn(
        UserFixture.createPrisonStaffUser(),
      )
      assertThat(userController.createLinkedGeneralUser(createLinkedGeneralUserRequest)).isNotNull
      verify(userService).createLinkedGeneralUser(createLinkedGeneralUserRequest)
    }
  }

  @Nested
  inner class FindUserByUsername {
    @Test
    fun `find user by user name`() {
      whenever(userService.findUserByUsername("NUSER_GEN")).thenReturn(
        UserFixture.createPrisonUserDetails(),
      )
      assertThat(userController.findUserByUsername("NUSER_GEN")).isNotNull
      verify(userService).findUserByUsername("NUSER_GEN")
    }
  }

  @Nested
  inner class FindUsersByFirstAndLastName {
    @Test
    fun `no matches`() {
      whenever(userService.findUsersByFirstAndLastName(anyString(), anyString())).thenReturn(listOf())
      assertThat(userController.findUsersByFirstAndLastName("first", "last")).isEmpty()
    }

    @Test
    fun `User mapped to PrisonUser`() {
      val user = EnhancedPrisonUser(
        verified = true,
        username = "username",
        email = "user@justice.gov.uk",
        firstName = "first",
        lastName = "last",
        userId = "123456789",
        activeCaseLoadId = "MDI",
      )
      whenever(userService.findUsersByFirstAndLastName(anyString(), anyString())).thenReturn(listOf(user))

      assertThat(userController.findUsersByFirstAndLastName("first", "last"))
        .containsExactly(
          PrisonUserDto(
            username = "username",
            staffId = 123456789,
            verified = true,
            email = "user@justice.gov.uk",
            firstName = "First",
            lastName = "Last",
            name = "First Last",
            activeCaseLoadId = "MDI",
          ),
        )
    }

    @Test
    fun `User mapped to PrisonUser handling missing values`() {
      val user = EnhancedPrisonUser(
        verified = false,
        username = "username",
        firstName = "first",
        lastName = "last",
        userId = "123456789",
        email = null,
        activeCaseLoadId = null,
      )
      whenever(userService.findUsersByFirstAndLastName(anyString(), anyString())).thenReturn(listOf(user))

      assertThat(userController.findUsersByFirstAndLastName("first", "last"))
        .containsExactly(
          PrisonUserDto(
            username = "username",
            staffId = 123456789,
            verified = false,
            firstName = "First",
            lastName = "Last",
            name = "First Last",
            email = null,
            activeCaseLoadId = null,
          ),
        )
    }
  }
}

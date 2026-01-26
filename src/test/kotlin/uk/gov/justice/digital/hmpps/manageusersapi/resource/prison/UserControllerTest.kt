package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserBasicDetails
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserSearchSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.EnhancedPrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.filter.PrisonUserFilter
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PageDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PageSort
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
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
        createPrisonUserDetails(),
      )
      assertThat(userController.findUserByUsername("NUSER_GEN")).isNotNull
      verify(userService).findUserByUsername("NUSER_GEN")
    }
  }

  @Nested
  inner class FindUsersByUsernames {
    @Test
    fun `find users by user names`() {
      val usernames = listOf("NUSER_GEN")
      val fixturePrisonUsers = usernames.associateBy({ username -> username }, { createPrisonUserBasicDetails() })
      whenever(userService.findUsersByUsernames(usernames)).thenReturn(fixturePrisonUsers)

      val users = userController.findUsersByUsernames(usernames)

      assertThat(users).hasSize(1)
      verify(userService).findUsersByUsernames(usernames)
    }
  }

  @Nested
  inner class FindUserDetailsByUsername {
    @Test
    fun `find user by user name`() {
      whenever(userService.findUserDetailsByUsername("NUSER_GEN")).thenReturn(
        UserFixture.createPrisonUserFullDetails(),
      )
      assertThat(userController.getUserDetails("NUSER_GEN")).isNotNull
      verify(userService).findUserDetailsByUsername("NUSER_GEN")
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

      assertThat(userController.findUsersByFirstAndLastName("first", "last")).containsExactly(
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

  @Nested
  inner class EnableUser {
    @Test
    fun `enable user service is called`() {
      whenever(userService.findUserByUsername("NUSER_GEN")).thenReturn(createPrisonUserDetails())
      doNothing().whenever(userService).enableUser("NUSER_GEN")

      userController.enableUser("NUSER_GEN")

      verify(userService).enableUser("NUSER_GEN")
    }
  }

  @Nested
  inner class DisableUser {
    @Test
    fun `enable user service is called`() {
      whenever(userService.findUserByUsername("NUSER_GEN")).thenReturn(createPrisonUserDetails())
      doNothing().whenever(userService).disableUser("NUSER_GEN")

      userController.disableUser("NUSER_GEN")

      verify(userService).disableUser("NUSER_GEN")
    }
  }

  @Nested
  inner class FindUsersByFilter {
    @Test
    fun `calls prisonUserService`() {
      val sort = PageSort(true, false, true)
      val response = PagedResponse(
        content = listOf(
          createPrisonUserSearchSummary(username = "user1"),
          createPrisonUserSearchSummary(username = "user2"),
        ),
        pageable = PageDetails(sort, 10, 1, 2, true, true),
        totalElements = 2,
        totalPages = 1,
        last = true,
        first = true,
        sort = sort,
        numberOfElements = 2,
        size = 2,
        number = 0,
        empty = false,
      )
      whenever(userService.findUsersByFilter(any(), any())).thenReturn(response)

      userService.findUsersByFilter(PageRequest.of(0, 10), PrisonUserFilter())

      verify(userService).findUsersByFilter(PageRequest.of(0, 10), PrisonUserFilter())
    }
  }

  @Nested
  inner class DownloadUsersByFilter {
    @Test
    fun `calls prisonUserService`() {
      whenever(userService.downloadUsersByFilter(any())).thenReturn(
        listOf(
          UserFixture.createPrisonUserDownloadSummary(username = "user1"),
          UserFixture.createPrisonUserDownloadSummary(username = "user2"),
        ),
      )

      userService.downloadUsersByFilter(PrisonUserFilter())

      verify(userService).downloadUsersByFilter(PrisonUserFilter())
    }
  }

  @Nested
  inner class DownloadPrisonAdminsByFilter {
    @Test
    fun `calls prisonUserService`() {
      whenever(userService.downloadPrisonAdminsByFilter(any())).thenReturn(
        listOf(
          UserFixture.createPrisonAdminUserSummary(username = "user1"),
          UserFixture.createPrisonAdminUserSummary(username = "user2"),
        ),
      )

      userService.downloadPrisonAdminsByFilter(PrisonUserFilter())

      verify(userService).downloadPrisonAdminsByFilter(PrisonUserFilter())
    }
  }
}

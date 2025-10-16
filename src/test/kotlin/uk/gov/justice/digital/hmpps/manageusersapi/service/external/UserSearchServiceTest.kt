package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.service.Status.ACTIVE
import java.util.UUID

class UserSearchServiceTest {

  private val userSearchApiService: UserSearchApiService = mock()
  private lateinit var userSearchService: UserSearchService

  @BeforeEach
  fun setUp() {
    userSearchService = UserSearchService(userSearchApiService)
  }

  @Nested
  inner class FindExternalUsersByCrsGroup {
    @Test
    fun shouldFindUsersByCrsGroupFromExternal() {
      val crsGroupCode = "CRS-GROUP-CODE"
      val expectedUser = givenAnExternalUser()
      whenever(userSearchApiService.findUsersByCrsGroup(crsGroupCode)).thenReturn(listOf(expectedUser))

      val actualUsers = userSearchService.findExternalUsersByCrsGroup(crsGroupCode)

      assertThat(actualUsers).containsExactly(expectedUser)
    }
  }

  @Nested
  inner class FindUsersByEmail {
    @Test
    fun shouldReturnNullWhenEmailNull() {
      assertThat(userSearchService.findExternalUsersByEmail(null)).isNull()
    }

    @Test
    fun shouldNotInvokeExternalUsersWhenEmailNull() {
      userSearchService.findExternalUsersByEmail(null)

      verify(userSearchApiService, never()).findUsersByEmail(any())
    }

    @Test
    fun shouldCallExternalUsersWhenEmailNotNull() {
      userSearchService.findExternalUsersByEmail("testy@testing.com")

      verify(userSearchApiService).findUsersByEmail("testy@testing.com")
    }
  }

  @Nested
  inner class FindUserByUsername {
    @Test
    fun shouldCallExternalUsersWhenUserNameNotNull() {
      userSearchService.findExternalUserByUsername("user")

      verify(userSearchApiService).findUserByUsername("user")
    }
  }

  @Nested
  inner class FindUserById {

    @Test
    fun shouldCallExternalUsersApi() {
      val expectedUser = givenAnExternalUser()
      whenever(userSearchApiService.findByUserId(expectedUser.userId)).thenReturn(expectedUser)

      val actualUser = userSearchService.findExternalUserById(expectedUser.userId)

      assertEquals(expectedUser, actualUser)
    }
  }

  @Nested
  inner class FindUsers {
    @Test
    fun shouldCallExternalUsers() {
      val name = "tester"
      val roles = listOf("role1")
      val groups = listOf("group1")

      userSearchService.findUsers(name, roles, groups, Pageable.unpaged(), ACTIVE)

      verify(userSearchApiService).findUsers(name, roles, groups, Pageable.unpaged(), ACTIVE)
    }
  }

  private fun givenAnExternalUser() = ExternalUser(userId = UUID.randomUUID(), username = "testy", email = "testy@testing.com", firstName = "Testy", lastName = "McTesting")
}

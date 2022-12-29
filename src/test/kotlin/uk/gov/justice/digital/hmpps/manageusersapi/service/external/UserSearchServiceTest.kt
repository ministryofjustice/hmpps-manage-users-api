package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.Status.ACTIVE

class UserSearchServiceTest {

  private val userSearchApiService: UserSearchApiService = mock()
  private lateinit var userSearchService: UserSearchService

  @BeforeEach
  fun setUp() {
    userSearchService = UserSearchService(userSearchApiService)
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
}

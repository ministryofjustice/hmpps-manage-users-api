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
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers.ExternalUsersApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.Status.ACTIVE

class UserSearchServiceTest {

  private val externalUsersApiService: ExternalUsersApiService = mock()
  private lateinit var userSearchService: UserSearchService

  @BeforeEach
  fun setUp() {
    userSearchService = UserSearchService(externalUsersApiService)
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

      verify(externalUsersApiService, never()).findUsersByEmail(any())
    }

    @Test
    fun shouldCallExternalUsersWhenEmailNotNull() {
      userSearchService.findExternalUsersByEmail("testy@testing.com")

      verify(externalUsersApiService).findUsersByEmail("testy@testing.com")
    }
  }

  @Nested
  inner class FindUserByUsername {
    @Test
    fun shouldCallExternalUsersWhenUserNameNotNull() {
      userSearchService.findExternalUserByUsername("user")

      verify(externalUsersApiService).findUserByUsername("user")
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

      verify(externalUsersApiService).findUsers(name, roles, groups, Pageable.unpaged(), ACTIVE)
    }
  }
}

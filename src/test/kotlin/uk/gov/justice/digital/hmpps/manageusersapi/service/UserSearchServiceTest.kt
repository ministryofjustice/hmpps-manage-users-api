package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.Status.ACTIVE

class UserSearchServiceTest {

  private val authApiService: AuthApiService = mock()
  private lateinit var userSearchService: UserSearchService

  @BeforeEach
  fun setUp() {
    userSearchService = UserSearchService(authApiService)
  }

  @Nested
  inner class FindUsers {
    @Test
    fun shouldCallAuthApi() {
      val name = "tester"

      userSearchService.searchUsers(name, ACTIVE, null, null, null, null)

      verify(authApiService).findUsers(name, ACTIVE, null, null, null, null)
    }
  }
}

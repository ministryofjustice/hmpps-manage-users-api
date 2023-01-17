package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import java.util.UUID

class UserServiceTest {
  private val externalUsersApiService: UserSearchApiService = mock()

  private val userService = UserService(
    externalUsersApiService
  )

  @Nested
  inner class FindUserByUsername {
    @Test
    fun `find external user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())

      val user = userService.findUserByUsername("external_user")
      assertThat(user).isPresent.get().extracting { it.username }.isEqualTo("external_user")
    }

    @Test
    fun `user not found`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      assertThat(userService.findUserByUsername("does_not_exist_user")).isEmpty
    }
  }

  fun createExternalUser() =
    ExternalUserDetailsDto(
      userId = UUID.randomUUID(),
      username = "external_user",
      email = "someemail@hello.com",
      firstName = "fred",
      lastName = "Smith"
    )
}

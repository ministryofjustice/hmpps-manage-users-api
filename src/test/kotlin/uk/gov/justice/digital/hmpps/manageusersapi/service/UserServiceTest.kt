package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.azuread
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import java.util.UUID

class UserServiceTest {
  private val externalUsersApiService: UserSearchApiService = mock()
  private val authApiService: AuthApiService = mock()

  private val userService = UserService(
    authApiService,
    externalUsersApiService
  )

  @Nested
  inner class FindUserByUsername {
    @Test
    fun `find external user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())

      val user = userService.findUserByUsername("external_user")
      assertThat(user?.username).isEqualTo("external_user")
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `find azure user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(createAzureUser())

      val user = userService.findUserByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(user?.username).isEqualTo("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(user?.name).isEqualTo("Azure User")
    }

    @Test
    fun `user not found`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)

      val user = userService.findUserByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      verify(authApiService).findAzureUserByUsername(anyString())
      assertThat(user).isNull()
    }
  }

  fun createAzureUser() =
    UserDetailsDto(
      username = "2E285CED-DCFD-4497-9E22-89E8E10A2A6A",
      active = true,
      name = "Azure User",
      authSource = azuread,
      userId = "azureuser@justice.gov.uk",
      uuid = UUID.randomUUID()
    )

  fun createExternalUser() =
    ExternalUserDetailsDto(
      userId = UUID.randomUUID(),
      username = "external_user",
      email = "someemail@hello.com",
      firstName = "fred",
      lastName = "Smith"
    )
}

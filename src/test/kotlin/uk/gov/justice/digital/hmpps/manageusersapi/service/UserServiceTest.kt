package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.azuread
import uk.gov.justice.digital.hmpps.manageusersapi.model.DeliusUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import java.util.UUID

class UserServiceTest {
  private val externalUsersApiService: UserSearchApiService = mock()
  private val authApiService: AuthApiService = mock()
  private val deliusUserApiService: UserApiService = mock()

  private val userService = UserService(
    authApiService,
    deliusUserApiService,
    externalUsersApiService
  )

  @Nested
  inner class FindUserByUsername {
    @Test
    fun `find external user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())

      val user = userService.findUserByUsername("external_user")
      assertThat(user?.username).isEqualTo("external_user")
      verifyNoInteractions(deliusUserApiService)
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `find delius user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(createDeliusUser())

      val user = userService.findUserByUsername("DELIUSUSER")
      assertThat(user?.username).isEqualTo("DELIUSUSER")
      assertThat(user?.name).isEqualTo("Delius Smith")
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `find azure user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(createAzureUser())

      val user = userService.findUserByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(user?.username).isEqualTo("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(user?.name).isEqualTo("Azure User")
    }

    @Test
    fun `user not found`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(null)
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

  fun createDeliusUser() =
    DeliusUserDetails(
      username = "DELIUSUSER",
      userId = "1234567890",
      firstName = "Delius",
      surname = "Smith",
      enabled = true
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

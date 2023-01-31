package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.azuread
import uk.gov.justice.digital.hmpps.manageusersapi.model.DeliusUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.NomisUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import java.util.UUID

class UserServiceTest {
  private val authApiService: AuthApiService = mock()
  private val deliusUserApiService: UserApiService = mock()
  private val externalUsersApiService: UserSearchApiService = mock()
  private val nomisUserApiService: uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()

  private val userService = UserService(
    authApiService,
    deliusUserApiService,
    externalUsersApiService,
    nomisUserApiService,
    authenticationFacade,
  )

  @Nested
  inner class FindUserByUsername {
    @Test
    fun `find external user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())

      val user = userService.findUserByUsername("external_user")
      assertThat(user?.username).isEqualTo("external_user")
      verifyNoInteractions(nomisUserApiService)
      verifyNoInteractions(deliusUserApiService)
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `find nomis user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(createNomisUser())

      val user = userService.findUserByUsername("NUSER_GEN")
      assertThat(user?.username).isEqualTo("NUSER_GEN")
      assertThat(user?.name).isEqualTo("Nomis Take")
      verifyNoInteractions(deliusUserApiService)
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `find delius user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(createDeliusUser())

      val user = userService.findUserByUsername("DELIUSUSER")
      assertThat(user?.username).isEqualTo("DELIUSUSER")
      assertThat(user?.name).isEqualTo("Delius Smith")
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `find azure user`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(createAzureUser())

      val user = userService.findUserByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(user?.username).isEqualTo("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(user?.name).isEqualTo("Azure User")
    }

    @Test
    fun `user not found`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)

      val user = userService.findUserByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      verify(authApiService).findAzureUserByUsername(anyString())
      assertThat(user).isNull()
    }

    @Nested
    inner class MyRoles {
      @Test
      fun myRoles() {
        whenever(authenticationFacade.authentication).thenReturn(authentication)
        whenever(authentication.authorities).thenReturn(listOf(SimpleGrantedAuthority("ROLE_BOB"), SimpleGrantedAuthority("ROLE_JOE_FRED")))
        assertThat(userService.myRoles()).containsOnly(ExternalUserRole("BOB"), ExternalUserRole("JOE_FRED"))
      }

      @Test
      fun myRoles_noRoles() {
        whenever(authenticationFacade.authentication).thenReturn(authentication)
        whenever(authentication.authorities).thenReturn(emptyList())
        assertThat(userService.myRoles()).isEmpty()
      }
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

  fun createNomisUser() =
    NomisUserDetails(
      username = "NUSER_GEN",
      staffId = "123456",
      firstName = "Nomis",
      lastName = "Take",
      activeCaseLoadId = "MDI",
      email = "nomis.usergen@digital.justice.gov.uk",
      enabled = true,
    )
}

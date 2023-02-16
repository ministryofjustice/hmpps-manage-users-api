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
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.auth
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.azuread
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.delius
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.nomis
import uk.gov.justice.digital.hmpps.manageusersapi.model.DeliusUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.NomisUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import java.util.UUID
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService as NomisUserApiService

class UserServiceTest {
  private val authApiService: AuthApiService = mock()
  private val deliusUserApiService: UserApiService = mock()
  private val externalUsersApiService: UserSearchApiService = mock()
  private val nomisUserApiService: NomisUserApiService = mock()
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
      val uuid = UUID.randomUUID()
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())
      whenever(authApiService.findUserByUsernameAndSource("external_user", auth)).thenReturn(createAuthUserDetails(uuid))

      val user = userService.findUserByUsername("external_user") as UserDetailsDto
      assertThat(user.username).isEqualTo("external_user")
      assertThat(user.uuid).isEqualTo(uuid)
      verifyNoInteractions(nomisUserApiService)
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find nomis user`() {
      val uuid = UUID.randomUUID()
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(createNomisUser())
      whenever(authApiService.findUserByUsernameAndSource("nuser_gen", nomis)).thenReturn(createAuthUserDetails(uuid))

      val user = userService.findUserByUsername("nuser_gen") as UserDetailsDto
      assertThat(user.username).isEqualTo("NUSER_GEN")
      assertThat(user.name).isEqualTo("Nomis Take")
      assertThat(user.uuid).isEqualTo(uuid)
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find azure user`() {
      val uuid = UUID.randomUUID()
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(createAzureUser())
      whenever(authApiService.findUserByUsernameAndSource("2E285CED-DCFD-4497-9E22-89E8E10A2A6A", azuread)).thenReturn((createAuthUserDetails(uuid)))

      val user = userService.findUserByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A") as UserDetailsDto
      assertThat(user.username).isEqualTo("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(user.name).isEqualTo("Azure User")
      assertThat(user.uuid).isEqualTo(uuid)
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find delius user`() {
      val uuid = UUID.randomUUID()
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(createDeliusUser())
      whenever(authApiService.findUserByUsernameAndSource("deliususer", delius)).thenReturn((createAuthUserDetails(uuid)))

      val user = userService.findUserByUsername("deliususer") as UserDetailsDto
      assertThat(user.username).isEqualTo("DELIUSUSER")
      assertThat(user.name).isEqualTo("Delius Smith")
      assertThat(user.uuid).isEqualTo(uuid)
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
    inner class MyDetails {
      @Test
      fun myDetails() {
        val userDetails = UserDetailsDto(
          "username",
          true,
          "Any User",
          auth,
          userId = UUID.randomUUID().toString(),
          uuid = UUID.randomUUID()
        )
        whenever(authenticationFacade.currentUsername).thenReturn("Test User")

        assertThat(userService.myDetails()).isNotEqualTo(userDetails)
      }

      @Test
      fun myDetails_basicUser() {
        val userDetails = UsernameDto("username")
        whenever(authenticationFacade.currentUsername).thenReturn("Test User")

        assertThat(userService.myDetails()).isNotEqualTo(userDetails)
      }
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
      username = "deliususer",
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

  fun createAuthUserDetails(uuid: UUID) = AuthUserDetails(uuid = uuid)
}

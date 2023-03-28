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
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserRolesApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createNomisUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.auth
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.azuread
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.delius
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.nomis
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.AzureUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.DeliusUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailAddress
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserRole
import java.util.UUID
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService as NomisUserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserRole as UserRoleResponse

class UserServiceTest {
  private val authApiService: AuthApiService = mock()
  private val deliusUserApiService: UserApiService = mock()
  private val externalUsersApiService: UserSearchApiService = mock()
  private val nomisUserApiService: NomisUserApiService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()
  private val externalRolesApiService: UserRolesApiService = mock()

  private val userService = UserService(
    authApiService,
    deliusUserApiService,
    externalUsersApiService,
    nomisUserApiService,
    authenticationFacade,
    externalRolesApiService,
  )

  @Nested
  inner class FindUserByUsername {
    @Test
    fun `find external user`() {
      val uuid = UUID.randomUUID()
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())
      whenever(authApiService.findUserByUsernameAndSource("external_user", auth)).thenReturn(createAuthUserDetails(uuid))

      val user = userService.findUserByUsername("external_user")
      assertThat(user!!.username).isEqualTo("external_user")
      assertThat(user.uuid).isEqualTo(uuid)
      verifyNoInteractions(nomisUserApiService)
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find nomis user`() {
      val uuid = UUID.randomUUID()
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(createNomisUserDetails())
      whenever(authApiService.findUserByUsernameAndSource("nuser_gen", nomis)).thenReturn(createAuthUserDetails(uuid))

      val user = userService.findUserByUsername("nuser_gen")
      assertThat(user!!.username).isEqualTo("NUSER_GEN")
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

      val user = userService.findUserByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(user!!.username).isEqualTo("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
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

      val user = userService.findUserByUsername("deliususer")
      assertThat(user!!.username).isEqualTo("DELIUSUSER")
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
  }

  @Nested
  inner class FindUserEmail {
    @Test
    fun `find user email - already exists in auth`() {
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(createEmailAddressVerified())
      val user = userService.findUserEmail("verified_user", true)
      assertThat(user!!.username).isEqualTo("verified_user")
      verifyNoInteractions(externalUsersApiService)
      verifyNoInteractions(nomisUserApiService)
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find user email only stored in external users`() {
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(null)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())
      whenever(authApiService.findUserByUsernameAndSource("external_user", auth)).thenReturn(
        createAuthUserDetails(),
      )

      val userEmail = userService.findUserEmail("external_user", true)
      assertThat(userEmail!!.username).isEqualTo("external_user")
      assertThat(userEmail.email).isEqualTo("someemail@hello.com")
      verifyNoInteractions(nomisUserApiService)
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find user email only stored in nomis`() {
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(null)
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(null)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(createNomisUserDetails())
      whenever(authApiService.findUserByUsernameAndSource("nuser_gen", nomis)).thenReturn(
        createAuthUserDetails(),
      )

      val userEmail = userService.findUserEmail("nuser_gen", true)
      assertThat(userEmail!!.username).isEqualTo("NUSER_GEN")
      assertThat(userEmail.email).isEqualTo("nomis.usergen@digital.justice.gov.uk")
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find user email only stored in azure`() {
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(null)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(createAzureUser())
      whenever(
        authApiService.findUserByUsernameAndSource(
          "2E285CED-DCFD-4497-9E22-89E8E10A2A6A",
          azuread,
        ),
      ).thenReturn((createAuthUserDetails()))

      val userEmail = userService.findUserEmail("2E285CED-DCFD-4497-9E22-89E8E10A2A6A", true)
      assertThat(userEmail!!.username).isEqualTo("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(userEmail.email).isEqualTo("azureuser@justice.gov.uk")
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find user email only stored in delius`() {
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(null)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(createDeliusUser())
      whenever(
        authApiService.findUserByUsernameAndSource(
          "deliususer",
          delius,
        ),
      ).thenReturn((createAuthUserDetails()))

      val userEmail = userService.findUserEmail("deliususer", true)
      assertThat(userEmail!!.username).isEqualTo("deliususer")
      assertThat(userEmail.email).isEqualTo("delius.smith@digital.justice.gov.uk")
    }

    @Test
    fun `user not found`() {
      whenever(authApiService.findAuthUserEmail("2E285CED-DCFD-4497-9E22-89E8E10A2A6A", true)).thenReturn(null)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)

      val user = userService.findUserByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      verify(authApiService).findAzureUserByUsername(anyString())
      assertThat(user).isNull()
    }
  }

  @Nested
  inner class UserRolesList {
    @Test
    fun `user not found`() {
      whenever(externalRolesApiService.findRolesByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)

      val userRoleList = userService.findRolesByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      verify(authApiService).findAzureUserByUsername(anyString())
      assertThat(userRoleList).isNull()
    }

    @Test
    fun `find roles of delius user`() {
      val uuid = UUID.randomUUID()
      whenever(externalRolesApiService.findRolesByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(createDeliusUser())
      whenever(authApiService.findUserByUsernameAndSource("deliususer", delius)).thenReturn((createAuthUserDetails(uuid)))

      val userRoleList = userService.findRolesByUsername("deliususer")
      assertThat(userRoleList).isEqualTo(listOf(UserRole(roleCode = "TEST_ROLE")))
    }

    @Test
    fun `find roles of azure user`() {
      whenever(externalRolesApiService.findRolesByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(createAzureUser())

      val userRoleList = userService.findRolesByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(userRoleList).isEmpty()
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find roles of nomis user`() {
      whenever(externalRolesApiService.findRolesByUsernameOrNull(anyString())).thenReturn(null)
      whenever(nomisUserApiService.findUserByUsername(anyString())).thenReturn(createNomisUserDetails())

      val userRoleList = userService.findRolesByUsername("nuser_gen")
      assertThat(userRoleList).isEqualTo(listOf(UserRole(roleCode = "ROLE1"), UserRole(roleCode = "ROLE2"), UserRole(roleCode = "ROLE3")))
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find roles of external user`() {
      whenever(externalRolesApiService.findRolesByUsernameOrNull(anyString())).thenReturn(createRolesList())

      val userRoleList = userService.findRolesByUsername("external_user")
      assertThat(userRoleList).isEqualTo(listOf(UserRole(roleCode = "AUDIT_VIEWER"), UserRole(roleCode = "AUTH_GROUP_MANAGER")))
      verifyNoInteractions(nomisUserApiService)
      verifyNoInteractions(deliusUserApiService)
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

  fun createAzureUser() =
    AzureUser(
      username = "2E285CED-DCFD-4497-9E22-89E8E10A2A6A",
      email = "azureuser@justice.gov.uk",
      enabled = true,
      firstName = "Azure",
      lastName = "User",
    )

  fun createDeliusUser() =
    DeliusUser(
      username = "deliususer",
      userId = "1234567890",
      firstName = "Delius",
      surname = "Smith",
      enabled = true,
      email = "delius.smith@digital.justice.gov.uk",
      roles = listOf(uk.gov.justice.digital.hmpps.manageusersapi.model.UserRole(name = "ROLE_TEST_ROLE")),
    )

  fun createExternalUser() = ExternalUser(
    userId = UUID.randomUUID(),
    username = "external_user",
    email = "someemail@hello.com",
    firstName = "fred",
    lastName = "Smith",
  )

  fun createEmailAddressVerified() = EmailAddress(
    username = "verified_user",
    email = "someemail@hello.com",
    verified = true,
  )

  fun createRolesList() = listOf(
    UserRoleResponse(
      roleCode = "AUDIT_VIEWER",
      roleName = "viewer",
    ),
    UserRoleResponse(
      roleCode = "AUTH_GROUP_MANAGER",
      roleName = "Auth Group Manager that has more than 30 characters in the role name",
      roleDescription = "Gives group manager ability to administer user in there groups",
    ),
  )

  fun createAuthUserDetails(uuid: UUID = UUID.randomUUID()) = AuthUser(uuid = uuid)
}

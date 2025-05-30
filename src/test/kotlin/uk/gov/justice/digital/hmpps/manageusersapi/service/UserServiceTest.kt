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
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.RolesApiService
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserBasicDetails
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.auth
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.azuread
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.delius
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.nomis
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.AzureUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.DeliusUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailAddress
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseloadDetail
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserGroupDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UsageType
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.UserGroupService
import java.util.UUID
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService as PrisonUserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserRole as UserRoleResponse

class UserServiceTest {
  private val authApiService: AuthApiService = mock()
  private val deliusUserApiService: UserApiService = mock()
  private val externalUsersApiService: UserSearchApiService = mock()
  private val prisonUserApiService: PrisonUserApiService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private val authentication: Authentication = mock()
  private val externalRolesApiService: UserRolesApiService = mock()
  private val userGroupsService: UserGroupService = mock()
  private val nomisRolesApiService: RolesApiService = mock()

  private val userService = UserService(
    authApiService,
    deliusUserApiService,
    externalUsersApiService,
    prisonUserApiService,
    authenticationFacade,
    externalRolesApiService,
    userGroupsService,
    nomisRolesApiService,
  )

  @Nested
  inner class FindUserByUsername {
    @Test
    fun `find external user`() {
      val uuid = UUID.randomUUID()
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())
      whenever(authApiService.findUserIdByUsernameAndSource("external_user", auth)).thenReturn(createAuthUserId(uuid))

      val user = userService.findUserByUsername("external_user")
      assertThat(user!!.username).isEqualTo("external_user")
      assertThat(user.uuid).isEqualTo(uuid)
      verifyNoInteractions(prisonUserApiService)
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find nomis user`() {
      val uuid = UUID.randomUUID()
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(prisonUserApiService.findUserBasicDetailsByUsername(anyString())).thenReturn(createPrisonUserBasicDetails())
      whenever(authApiService.findUserIdByUsernameAndSource("nuser_gen", nomis)).thenReturn(createAuthUserId(uuid))

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
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(createAzureUser())
      whenever(authApiService.findUserIdByUsernameAndSource("2E285CED-DCFD-4497-9E22-89E8E10A2A6A", azuread)).thenReturn((createAuthUserId(uuid)))

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
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(createDeliusUser())
      whenever(authApiService.findUserIdByUsernameAndSource("deliususer", delius)).thenReturn((createAuthUserId(uuid)))

      val user = userService.findUserByUsername("deliususer")
      assertThat(user!!.username).isEqualTo("DELIUSUSER")
      assertThat(user.name).isEqualTo("Delius Smith")
      assertThat(user.uuid).isEqualTo(uuid)
    }

    @Test
    fun `user not found`() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)
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
      verifyNoInteractions(prisonUserApiService)
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find user email only stored in external users`() {
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(null)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())
      whenever(authApiService.findUserIdByUsernameAndSource("external_user", auth)).thenReturn(
        createAuthUserId(),
      )

      val userEmail = userService.findUserEmail("external_user", true)
      assertThat(userEmail!!.username).isEqualTo("external_user")
      assertThat(userEmail.email).isEqualTo("someemail@hello.com")
      verifyNoInteractions(prisonUserApiService)
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find user email only stored in nomis`() {
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(null)
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(null)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(createPrisonUserDetails())
      whenever(authApiService.findUserIdByUsernameAndSource("nuser_gen", nomis)).thenReturn(
        createAuthUserId(),
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
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(createAzureUser())
      whenever(
        authApiService.findUserIdByUsernameAndSource(
          "2E285CED-DCFD-4497-9E22-89E8E10A2A6A",
          azuread,
        ),
      ).thenReturn((createAuthUserId()))

      val userEmail = userService.findUserEmail("2E285CED-DCFD-4497-9E22-89E8E10A2A6A", true)
      assertThat(userEmail!!.username).isEqualTo("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(userEmail.email).isEqualTo("azureuser@justice.gov.uk")
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find user email only stored in delius`() {
      whenever(authApiService.findAuthUserEmail("verified_user", true)).thenReturn(null)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(createDeliusUser())
      whenever(
        authApiService.findUserIdByUsernameAndSource(
          "deliususer",
          delius,
        ),
      ).thenReturn((createAuthUserId()))

      val userEmail = userService.findUserEmail("deliususer", true)
      assertThat(userEmail!!.username).isEqualTo("deliususer")
      assertThat(userEmail.email).isEqualTo("delius.smith@digital.justice.gov.uk")
    }

    @Test
    fun `user not found`() {
      whenever(authApiService.findAuthUserEmail("2E285CED-DCFD-4497-9E22-89E8E10A2A6A", true)).thenReturn(null)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)
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
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)
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
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(null)
      whenever(deliusUserApiService.findUserByUsername(anyString())).thenReturn(createDeliusUser())
      whenever(authApiService.findUserIdByUsernameAndSource("deliususer", delius)).thenReturn((createAuthUserId(uuid)))

      val userRoleList = userService.findRolesByUsername("deliususer")
      assertThat(userRoleList).isEqualTo(listOf(UserRole(roleCode = "TEST_ROLE")))
    }

    @Test
    fun `find roles of azure user`() {
      whenever(externalRolesApiService.findRolesByUsernameOrNull(anyString())).thenReturn(null)
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)
      whenever(authApiService.findAzureUserByUsername(anyString())).thenReturn(createAzureUser())

      val userRoleList = userService.findRolesByUsername("2E285CED-DCFD-4497-9E22-89E8E10A2A6A")
      assertThat(userRoleList).isEmpty()
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find roles of nomis user`() {
      whenever(externalRolesApiService.findRolesByUsernameOrNull(anyString())).thenReturn(null)
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(createPrisonUserDetails())

      val userRoleList = userService.findRolesByUsername("nuser_gen")
      assertThat(userRoleList).isEqualTo(listOf(UserRole(roleCode = "ROLE1"), UserRole(roleCode = "ROLE2"), UserRole(roleCode = "ROLE3")))
      verifyNoInteractions(deliusUserApiService)
    }

    @Test
    fun `find roles of external user`() {
      whenever(externalRolesApiService.findRolesByUsernameOrNull(anyString())).thenReturn(createRolesList())

      val userRoleList = userService.findRolesByUsername("external_user")
      assertThat(userRoleList).isEqualTo(listOf(UserRole(roleCode = "AUDIT_VIEWER"), UserRole(roleCode = "AUTH_GROUP_MANAGER")))
      verifyNoInteractions(prisonUserApiService)
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

  @Nested
  inner class MyGroups {
    @Test
    fun myGroups() {
      val uuid = UUID.randomUUID()
      var userGroupList = listOf(UserGroup("group_code", "Group name"))
      whenever(userGroupsService.getUserGroups(uuid, true)).thenReturn(userGroupList)
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser(uuid))
      whenever(authApiService.findUserIdByUsernameAndSource("external_user", auth)).thenReturn(createAuthUserId(uuid))
      assertThat(userService.findGroupDetails("external_user")).containsOnly(UserGroupDto("group_code", "Group name"))
    }

    @Test
    fun myGroups_noLocations() {
      val uuid = UUID.randomUUID()
      whenever(userGroupsService.getUserGroups(uuid, true)).thenReturn(emptyList())
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(createExternalUser())
      whenever(authApiService.findUserIdByUsernameAndSource("external_user", auth)).thenReturn(createAuthUserId(uuid))
      assertThat(userService.findGroupDetails("external_user")).isEmpty()
    }

    @Test
    fun invalidUser_noLocations() {
      whenever(externalUsersApiService.findUserByUsernameOrNull(anyString())).thenReturn(null)
      var myLocations = userService.findGroupDetails("external_user")
      assertThat(myLocations).isEmpty()
    }
  }

  @Nested
  inner class MyCaseloads {
    @Test
    fun myCaseloads() {
      whenever(nomisRolesApiService.getCaseloads()).thenReturn(createdUserCaseloadDetail())
      assertThat(userService.getCaseloads()).isEqualTo(createdUserCaseloadDetail())
    }
  }

  fun createAzureUser() = AzureUser(
    username = "2E285CED-DCFD-4497-9E22-89E8E10A2A6A",
    email = "azureuser@justice.gov.uk",
    enabled = true,
    firstName = "Azure",
    lastName = "User",
  )

  fun createDeliusUser() = DeliusUser(
    username = "deliususer",
    userId = "1234567890",
    firstName = "Delius",
    surname = "Smith",
    enabled = true,
    email = "delius.smith@digital.justice.gov.uk",
    roles = listOf("ROLE_TEST_ROLE"),
  )

  fun createExternalUser() = ExternalUser(
    userId = UUID.randomUUID(),
    username = "external_user",
    email = "someemail@hello.com",
    firstName = "fred",
    lastName = "Smith",
  )

  fun createExternalUser(userId: UUID) = ExternalUser(
    userId = userId,
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

  fun createAuthUserId(uuid: UUID = UUID.randomUUID()) = AuthUser(uuid = uuid)

  fun createdUserCaseloadDetail() = UserCaseloadDetail(
    username = "the username",
    active = true,
    accountType = UsageType.GENERAL,
    activeCaseload = PrisonCaseload(id = "WWI", name = "WANDSWORTH (HMP)"),
    caseloads = listOf(PrisonCaseload(id = "WWI", name = "WANDSWORTH (HMP)")),
  )
}

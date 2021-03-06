package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RolesPageable
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RolesPaged
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RolesSort
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.EXT_ADM

class RolesServiceTest {
  private val authService: AuthService = mock()
  private val nomisService: NomisApiService = mock()
  private val rolesService = RolesService(nomisService, authService)

  @Nested
  inner class GetAllRoles {
    @Test
    fun `get all roles`() {
      val roles = listOf(
        Role("ROLE_1", "Role 1", " description 1", listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))),
        Role("ROLE_2", "Role 2", " description 2", listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))),
      )
      whenever(authService.getRoles(isNull())).thenReturn(roles)

      val allRoles = authService.getRoles(null)
      assertThat(allRoles).isEqualTo(roles)
      verifyNoMoreInteractions(nomisService)
    }
  }

  @Nested
  inner class GetAllPagedRoles {
    @Test
    fun `get all paged roles`() {
      val roles = createRolePaged()

      whenever(authService.getPagedRoles(anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull())).thenReturn(roles)

      val allRoles = authService.getPagedRoles(3, 4, "roleName,asc", null, null, null)
      assertThat(allRoles).isEqualTo(roles)
      verifyNoMoreInteractions(nomisService)
    }
  }

  @Nested
  inner class GetRoleDetails {
    @Test
    fun `get role details`() {
      val role = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )

      whenever(authService.getRoleDetail(anyString())).thenReturn(role)
      val roleDetails = rolesService.getRoleDetail("RC1")

      assertThat(roleDetails).isEqualTo(role)
      verifyNoMoreInteractions(nomisService)
    }
  }

  @Nested
  inner class CreateARole {
    @Test
    fun `create external role`() {
      val role = CreateRole("ROLE_1", "Role Name", "A Role", setOf(EXT_ADM))

      rolesService.createRole(role)
      verify(authService).createRole(role)
      verifyNoMoreInteractions(nomisService)
    }

    @Test
    fun `create DPS only role`() {
      val role = CreateRole("ROLE_1", "Role Name", "A Role", setOf(DPS_ADM))

      rolesService.createRole(role)
      verify(authService).createRole(role)
      verify(nomisService).createRole(role)
    }

    @Test
    fun `create DPS and External role`() {
      val role = CreateRole("ROLE_1", "Role Name", "A Role", setOf(DPS_ADM))

      rolesService.createRole(role)
      verify(authService).createRole(role)
      verify(nomisService).createRole(role)
    }
  }

  @Nested
  inner class AmendRoleName {
    @Test
    fun `update role name when DPS Role`() {
      val roleAmendment = RoleNameAmendment("UpdatedName")

      val dbRole = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )
      whenever(authService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleName("ROLE_1", roleAmendment)
      verify(authService).updateRoleName("ROLE_1", roleAmendment)
      verify(nomisService).updateRoleName("ROLE_1", roleAmendment)
    }

    @Test
    fun `update role name when Not DPS Role`() {
      val roleAmendment = RoleNameAmendment("UpdatedName")

      val role = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))
      )
      whenever(authService.getRoleDetail("ROLE_1")).thenReturn(role)

      rolesService.updateRoleName("ROLE_1", roleAmendment)
      verify(authService).updateRoleName("ROLE_1", roleAmendment)
      verifyNoInteractions(nomisService)
    }
  }

  @Nested
  inner class AmendRoleAdminType {
    @Test
    fun `update role admin type for External Role to also be DPS Role`() {
      val roleAmendment = RoleAdminTypeAmendment(setOf(EXT_ADM, DPS_ADM))

      val dbRole = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))
      )
      whenever(authService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(authService).updateRoleAdminType("ROLE_1", roleAmendment)
      verify(nomisService).createRole(CreateRole("ROLE_1", "Role Name", "A Role", setOf(EXT_ADM, DPS_ADM)))
    }

    @Test
    fun `update role admin type for DPS Role to also be External Role`() {
      val roleAmendment = RoleAdminTypeAmendment(setOf(EXT_ADM, DPS_ADM))

      val dbRole = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )
      whenever(authService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(authService).updateRoleAdminType("ROLE_1", roleAmendment)
      verifyNoMoreInteractions(nomisService)
    }

    @Test
    fun `update role admin type for DPS LSA Role to just be DPS Admin Role`() {
      val roleAmendment = RoleAdminTypeAmendment(setOf(DPS_ADM, DPS_LSA))

      val dbRole = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(
          AdminTypeReturn("DPS_ADM", "DPS Central Administrator")
        )
      )
      whenever(authService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(authService).updateRoleAdminType("ROLE_1", roleAmendment)
      verify(nomisService).updateRoleAdminType("ROLE_1", roleAmendment)
    }

    @Test
    fun `update role admin type for DPS Admin Role to just be DPS LSA Role`() {
      val roleAmendment = RoleAdminTypeAmendment(setOf(DPS_ADM))

      val dbRole = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(
          AdminTypeReturn("DPS_ADM", "DPS Central Administrator"),
          AdminTypeReturn("DPS_LSA", "DPS Local System Administrator")
        )
      )
      whenever(authService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(authService).updateRoleAdminType("ROLE_1", roleAmendment)
      verify(nomisService).updateRoleAdminType("ROLE_1", roleAmendment)
    }
  }

  private fun createRoleSort() = RolesSort(sorted = true, unsorted = false, empty = false)
  private fun createRolesPageable() = RolesPageable(
    sort = createRoleSort(),
    offset = 12,
    pageNumber = 3,
    pageSize = 4,
    paged = true,
    unpaged = false
  )

  fun createRolePaged() = RolesPaged(
    content = listOf(
      Role("ROLE_1", "Role 1", " description 1", listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))),
      Role("ROLE_2", "Role 2", " description 2", listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))),
    ),
    pageable = createRolesPageable(),
    last = false,
    totalPages = 12,
    totalElements = 37,
    size = 4,
    number = 3,
    sort = createRoleSort(),
    numberOfElements = 37,
    first = false,
    empty = false
  )
}

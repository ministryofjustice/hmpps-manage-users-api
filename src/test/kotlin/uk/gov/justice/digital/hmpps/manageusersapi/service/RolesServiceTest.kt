package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.EXT_ADM

class RolesServiceTest {
  private val authService: AuthService = mock()
  private val nomisService: NomisApiService = mock()
  private val rolesService = RolesService(authService, nomisService)

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

      val dbRole = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))
      )
      whenever(authService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleName("ROLE_1", roleAmendment)
      verify(authService).updateRoleName("ROLE_1", roleAmendment)
      verifyNoMoreInteractions(nomisService)
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
}

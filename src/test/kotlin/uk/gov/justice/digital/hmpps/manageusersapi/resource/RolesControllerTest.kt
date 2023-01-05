package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminTypeReturn
import uk.gov.justice.digital.hmpps.manageusersapi.service.RolesService

class RolesControllerTest {

  private val rolesService: RolesService = mock()
  private val rolesController = RolesController(rolesService)

  @Nested
  inner class CreateRole {
    @Test
    fun `create role`() {
      val role = CreateRole("RO1", "Role1", "First Role", setOf(AdminType.EXT_ADM))
      rolesController.createRole(role)
      verify(rolesService).createRole(role)
    }

    @Test
    fun `create role remove ROLE_`() {
      val role = CreateRole("ROLE_RO1", "Role1", "First Role", setOf(AdminType.EXT_ADM))
      rolesController.createRole(role)
      verify(rolesService).createRole(role)
      verify(rolesService).createRole(
        argThat { r ->
          assertThat(r).isNotNull
          assertThat(r.roleCode).isEqualTo("RO1")
          true
        }
      )
    }
  }

  @Nested
  inner class GetAllRoles {
    @Test
    fun `Get all roles`() {
      rolesController.getRoles(null)
      verify(rolesService).getRoles(null)
    }

    @Test
    fun `Get all roles with filters`() {
      rolesController.getRoles(listOf(DPS_ADM))
      verify(rolesService).getRoles(listOf(DPS_ADM))
    }
  }

  @Nested
  inner class GetAllPagedRoles {
    @Test
    fun `Get all roles`() {

      rolesController.getPagedRoles(0, 10, "roleName,asc", null, null, null)
      verify(rolesService).getPagedRoles(0, 10, "roleName,asc", null, null, null)
    }

    @Test
    fun `Get all roles with filters`() {

      rolesController.getPagedRoles(0, 10, "roleName,asc", "HWPV", "HW", listOf(DPS_ADM))
      verify(rolesService).getPagedRoles(0, 10, "roleName,asc", "HWPV", "HW", listOf(DPS_ADM))
    }
  }

  @Nested
  inner class RoleDetail {
    @Test
    fun `Get role details`() {
      val role = Role(
        roleCode = "RO1",
        roleName = "Role1",
        roleDescription = "First Role",
        adminType = listOf(AdminTypeReturn("Code", "Name"))
      )

      whenever(rolesService.getRoleDetail(any())).thenReturn(role)

      val roleDetails = rolesController.getRoleDetail("RO1")
      assertThat(roleDetails).isEqualTo(
        Role(
          roleCode = "RO1",
          roleName = "Role1",
          roleDescription = "First Role",
          adminType = listOf(AdminTypeReturn("Code", "Name"))
        )
      )
    }
  }

  @Nested
  inner class AmendRoleName {
    @Test
    fun `amend role name`() {
      val roleAmendment = RoleNameAmendment("role")
      rolesController.amendRoleName("role1", roleAmendment)
      verify(rolesService).updateRoleName("role1", roleAmendment)
    }
  }

  @Nested
  inner class AmendRoleDescription {
    @Test
    fun `amend role description`() {
      val roleAmendment = RoleDescriptionAmendment("roleDesc")
      rolesController.amendRoleDescription("role1", roleAmendment)
      verify(rolesService).updateRoleDescription("role1", roleAmendment)
    }

    @Test
    fun `amend role description if no description set`() {
      val roleAmendment = RoleDescriptionAmendment(null)
      rolesController.amendRoleDescription("role1", roleAmendment)
      verify(rolesService).updateRoleDescription("role1", roleAmendment)
    }
  }

  @Nested
  inner class AmendRoleAdminType {
    @Test
    fun `amend role admin type`() {
      val roleAmendment = RoleAdminTypeAmendment(setOf(DPS_ADM))
      rolesController.amendRoleAdminType("role1", roleAmendment)
      verify(rolesService).updateRoleAdminType("role1", roleAmendment)
    }
  }
}

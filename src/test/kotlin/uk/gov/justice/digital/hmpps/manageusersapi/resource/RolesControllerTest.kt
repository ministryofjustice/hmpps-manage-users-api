package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminTypeReturn
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleExistsException
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.RolesService

class RolesControllerTest {

  private val rolesService: RolesService = mock()
  private val rolesController = RolesController(rolesService)

  @Nested
  inner class CreateRole {
    @Test
    fun`create role`() {
      val role = CreateRole("RO1", "Role1", "First Role", setOf(AdminType.EXT_ADM))
      rolesController.createRole(role)
      verify(rolesService).createRole(role)
    }

    @Test
    fun `amend role name with no match throws exception`() {
      whenever(rolesService.createRole(any())).thenThrow(
        RoleExistsException(
          "RO1",
          "role code already exists"
        )
      )
      val role = CreateRole("RO1", "Role1", "First Role", setOf(AdminType.EXT_ADM))

      assertThatThrownBy { rolesController.createRole(role) }
        .isInstanceOf(RoleExistsException::class.java)
        .withFailMessage("Unable to create role: RO1 with reason: role code already exists")
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

    @Test
    fun `Get role details with no match throws exception`() {
      whenever(rolesService.getRoleDetail(any())).thenThrow(RoleNotFoundException("find", "NoRole", "not found"))

      assertThatThrownBy { rolesController.getRoleDetail("ROLE_DOES_NOT_EXIST") }
        .isInstanceOf(RoleNotFoundException::class.java)
        .withFailMessage("Unable to find role: NoRole with reason: not found")
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

    @Test
    fun `amend role name with no match throws exception`() {
      whenever(rolesService.updateRoleName(anyString(), any())).thenThrow(
        RoleNotFoundException(
          "find",
          "NoRole",
          "not found"
        )
      )
      val roleAmendment = RoleNameAmendment("role")

      assertThatThrownBy { rolesController.amendRoleName("NoRole", roleAmendment) }
        .isInstanceOf(RoleNotFoundException::class.java)
        .withFailMessage("Unable to find role: NoRole with reason: not found")
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

    @Test
    fun `amend role description with no match throws exception`() {
      whenever(rolesService.updateRoleDescription(anyString(), any())).thenThrow(RoleNotFoundException("find", "NoRole", "not found"))
      val roleAmendment = RoleDescriptionAmendment("role description")

      assertThatThrownBy { rolesController.amendRoleDescription("NoRole", roleAmendment) }
        .isInstanceOf(RoleNotFoundException::class.java)
        .withFailMessage("Unable to find role: NoRole with reason: not found")
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

    @Test
    fun `amend role admin type with no match throws exception`() {
      whenever(rolesService.updateRoleAdminType(anyString(), any())).thenThrow(RoleNotFoundException("find", "NoRole", "not found"))
      val roleAmendment = RoleAdminTypeAmendment(setOf(DPS_ADM))

      assertThatThrownBy { rolesController.amendRoleAdminType("NoRole", roleAmendment) }
        .isInstanceOf(RoleNotFoundException::class.java)
        .withFailMessage("Unable to find role: NoRole with reason: not found")
    }
  }
}

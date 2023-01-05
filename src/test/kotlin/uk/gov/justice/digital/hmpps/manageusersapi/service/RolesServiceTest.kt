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
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.RolesApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PageDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PageSort
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.EXT_ADM

class RolesServiceTest {
  private val nomisRolesApiService: uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.RolesApiService = mock()
  private val externalRolesApiService: RolesApiService = mock()
  private val rolesService = RolesService(nomisRolesApiService, externalRolesApiService)

  @Nested
  inner class GetAllRoles {
    @Test
    fun `get all roles`() {
      val roles = listOf(
        Role("ROLE_1", "Role 1", " description 1", listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))),
        Role("ROLE_2", "Role 2", " description 2", listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))),
      )
      whenever(externalRolesApiService.getRoles(isNull())).thenReturn(roles)

      val allRoles = externalRolesApiService.getRoles(null)
      assertThat(allRoles).isEqualTo(roles)
      verifyNoMoreInteractions(nomisRolesApiService)
    }
  }

  @Nested
  inner class GetAllPagedRoles {
    @Test
    fun `get all paged roles`() {
      val roles = createRolePaged()

      whenever(
        externalRolesApiService.getPagedRoles(
          anyInt(),
          anyInt(),
          anyString(),
          isNull(),
          isNull(),
          isNull()
        )
      ).thenReturn(roles)

      val allRoles = externalRolesApiService.getPagedRoles(3, 4, "roleName,asc", null, null, null)
      assertThat(allRoles).isEqualTo(roles)
      verifyNoMoreInteractions(nomisRolesApiService)
    }
  }

  @Nested
  inner class GetRoleDetails {
    @Test
    fun `get role details`() {
      val role = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )

      whenever(externalRolesApiService.getRoleDetail(anyString())).thenReturn(role)
      val roleDetails = rolesService.getRoleDetail("RC1")

      assertThat(roleDetails).isEqualTo(role)
      verifyNoMoreInteractions(nomisRolesApiService)
    }
  }

  @Nested
  inner class CreateARole {
    @Test
    fun `create external role`() {
      val role = CreateRole("ROLE_1", "Role Name", "A Role", setOf(EXT_ADM))

      rolesService.createRole(role)
      verify(externalRolesApiService).createRole(role)
      verifyNoMoreInteractions(nomisRolesApiService)
    }

    @Test
    fun `create DPS only role`() {
      val role = CreateRole("ROLE_1", "Role Name", "A Role", setOf(DPS_ADM))

      rolesService.createRole(role)
      verify(externalRolesApiService).createRole(role)
      verify(nomisRolesApiService).createRole(role)
    }

    @Test
    fun `create DPS and External role`() {
      val role = CreateRole("ROLE_1", "Role Name", "A Role", setOf(DPS_ADM))

      rolesService.createRole(role)
      verify(externalRolesApiService).createRole(role)
      verify(nomisRolesApiService).createRole(role)
    }
  }

  @Nested
  inner class AmendRoleName {
    @Test
    fun `update role name when DPS Role`() {
      val roleAmendment = RoleNameAmendment("UpdatedName")

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleName("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleName("ROLE_1", roleAmendment)
      verify(nomisRolesApiService).updateRoleName("ROLE_1", roleAmendment)
    }

    @Test
    fun `update role name when Not DPS Role`() {
      val roleAmendment = RoleNameAmendment("UpdatedName")

      val role = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(role)

      rolesService.updateRoleName("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleName("ROLE_1", roleAmendment)
      verifyNoInteractions(nomisRolesApiService)
    }
  }

  @Nested
  inner class AmendRoleDescription {
    @Test
    fun `update role description when DPS Role`() {
      val roleAmendment = RoleDescriptionAmendment("UpdatedDescription")

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleDescription("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleDescription("ROLE_1", roleAmendment)
      verifyNoInteractions(nomisRolesApiService)
    }

    @Test
    fun `update role description when Not DPS Role`() {
      val roleAmendment = RoleDescriptionAmendment("UpdatedDescription")

      val role = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(role)

      rolesService.updateRoleDescription("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleDescription("ROLE_1", roleAmendment)
      verifyNoInteractions(nomisRolesApiService)
    }
  }

  @Nested
  inner class AmendRoleAdminType {
    @Test
    fun `update role admin type for External Role to also be DPS Role`() {
      val roleAmendment = RoleAdminTypeAmendment(setOf(EXT_ADM, DPS_ADM))

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
      verify(nomisRolesApiService).createRole(CreateRole("ROLE_1", "Role Name", "A Role", setOf(EXT_ADM, DPS_ADM)))
    }

    @Test
    fun `update role admin type for DPS Role to also be External Role`() {
      val roleAmendment = RoleAdminTypeAmendment(setOf(EXT_ADM, DPS_ADM))

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
      verifyNoMoreInteractions(nomisRolesApiService)
    }

    @Test
    fun `update role admin type for DPS LSA Role to just be DPS Admin Role`() {
      val roleAmendment = RoleAdminTypeAmendment(setOf(DPS_ADM, DPS_LSA))

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(
          AdminTypeReturn("DPS_ADM", "DPS Central Administrator")
        )
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
      verify(nomisRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
    }

    @Test
    fun `update role admin type for DPS Admin Role to just be DPS LSA Role`() {
      val roleAmendment = RoleAdminTypeAmendment(setOf(DPS_ADM))

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(
          AdminTypeReturn("DPS_ADM", "DPS Central Administrator"),
          AdminTypeReturn("DPS_LSA", "DPS Local System Administrator")
        )
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
      verify(nomisRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
    }
  }

  private fun createRoleSort() = PageSort(sorted = true, unsorted = false, empty = false)
  private fun createRolesPageable() = PageDetails(
    sort = createRoleSort(),
    offset = 12,
    pageNumber = 3,
    pageSize = 4,
    paged = true,
    unpaged = false
  )

  fun createRolePaged() = PagedResponse(
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

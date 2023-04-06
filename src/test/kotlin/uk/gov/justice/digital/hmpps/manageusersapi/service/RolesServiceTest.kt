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
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType.EXT_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminTypeReturn
import uk.gov.justice.digital.hmpps.manageusersapi.model.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRoleDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PageDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PageSort
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendmentDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendmentDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendmentDto

class RolesServiceTest {
  private val prisonRolesApiService: uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.RolesApiService = mock()
  private val externalRolesApiService: RolesApiService = mock()
  private val rolesService = RolesService(prisonRolesApiService, externalRolesApiService)

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
      verifyNoMoreInteractions(prisonRolesApiService)
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
          isNull(),
        ),
      ).thenReturn(roles)

      val allRoles = externalRolesApiService.getPagedRoles(3, 4, "roleName,asc", null, null, null)
      assertThat(allRoles).isEqualTo(roles)
      verifyNoMoreInteractions(prisonRolesApiService)
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
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator")),
      )

      whenever(externalRolesApiService.getRoleDetail(anyString())).thenReturn(role)
      val roleDetails = rolesService.getRoleDetail("RC1")

      assertThat(roleDetails).isEqualTo(role)
      verifyNoMoreInteractions(prisonRolesApiService)
    }
  }

  @Nested
  inner class CreateARole {
    @Test
    fun `create external role`() {
      val role = CreateRoleDto("ROLE_1", "Role Name", "A Role", setOf(EXT_ADM))

      rolesService.createRole(role)
      verify(externalRolesApiService).createRole(role)
      verifyNoMoreInteractions(prisonRolesApiService)
    }

    @Test
    fun `create DPS only role`() {
      val role = CreateRoleDto("ROLE_1", "Role Name", "A Role", setOf(DPS_ADM))

      rolesService.createRole(role)
      verify(externalRolesApiService).createRole(role)
      verify(prisonRolesApiService).createRole(role)
    }

    @Test
    fun `create DPS and External role`() {
      val role = CreateRoleDto("ROLE_1", "Role Name", "A Role", setOf(DPS_ADM))

      rolesService.createRole(role)
      verify(externalRolesApiService).createRole(role)
      verify(prisonRolesApiService).createRole(role)
    }
  }

  @Nested
  inner class AmendRoleName {
    @Test
    fun `update role name when DPS Role`() {
      val roleAmendment = RoleNameAmendmentDto("UpdatedName")

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator")),
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleName("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleName("ROLE_1", roleAmendment)
      verify(prisonRolesApiService).updateRoleName("ROLE_1", roleAmendment)
    }

    @Test
    fun `update role name when Not DPS Role`() {
      val roleAmendment = RoleNameAmendmentDto("UpdatedName")

      val role = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator")),
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(role)

      rolesService.updateRoleName("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleName("ROLE_1", roleAmendment)
      verifyNoInteractions(prisonRolesApiService)
    }
  }

  @Nested
  inner class AmendRoleDescription {
    @Test
    fun `update role description when DPS Role`() {
      val roleAmendment = RoleDescriptionAmendmentDto("UpdatedDescription")

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator")),
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleDescription("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleDescription("ROLE_1", roleAmendment)
      verifyNoInteractions(prisonRolesApiService)
    }

    @Test
    fun `update role description when Not DPS Role`() {
      val roleAmendment = RoleDescriptionAmendmentDto("UpdatedDescription")

      val role = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator")),
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(role)

      rolesService.updateRoleDescription("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleDescription("ROLE_1", roleAmendment)
      verifyNoInteractions(prisonRolesApiService)
    }
  }

  @Nested
  inner class AmendRoleAdminType {
    @Test
    fun `update role admin type for External Role to also be DPS Role`() {
      val roleAmendment = RoleAdminTypeAmendmentDto(setOf(EXT_ADM, DPS_ADM))

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator")),
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
      verify(prisonRolesApiService).createRole(CreateRoleDto("ROLE_1", "Role Name", "A Role", setOf(EXT_ADM, DPS_ADM)))
    }

    @Test
    fun `update role admin type for DPS Role to also be External Role`() {
      val roleAmendment = RoleAdminTypeAmendmentDto(setOf(EXT_ADM, DPS_ADM))

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator")),
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
      verifyNoMoreInteractions(prisonRolesApiService)
    }

    @Test
    fun `update role admin type for DPS LSA Role to just be DPS Admin Role`() {
      val roleAmendment = RoleAdminTypeAmendmentDto(setOf(DPS_ADM, DPS_LSA))

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(
          AdminTypeReturn("DPS_ADM", "DPS Central Administrator"),
        ),
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
      verify(prisonRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
    }

    @Test
    fun `update role admin type for DPS Admin Role to just be DPS LSA Role`() {
      val roleAmendment = RoleAdminTypeAmendmentDto(setOf(DPS_ADM))

      val dbRole = Role(
        roleCode = "ROLE_1",
        roleName = "Role Name",
        roleDescription = "A Role",
        adminType = listOf(
          AdminTypeReturn("DPS_ADM", "DPS Central Administrator"),
          AdminTypeReturn("DPS_LSA", "DPS Local System Administrator"),
        ),
      )
      whenever(externalRolesApiService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleAdminType("ROLE_1", roleAmendment)
      verify(externalRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
      verify(prisonRolesApiService).updateRoleAdminType("ROLE_1", roleAmendment)
    }
  }

  private fun createRoleSort() = PageSort(sorted = true, unsorted = false, empty = false)
  private fun createRolesPageable() = PageDetails(
    sort = createRoleSort(),
    offset = 12,
    pageNumber = 3,
    pageSize = 4,
    paged = true,
    unpaged = false,
  )

  fun createRolePaged() = PagedResponse(
    content = listOf(
      RoleDto("ROLE_1", "Role 1", " description 1", listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))),
      RoleDto("ROLE_2", "Role 2", " description 2", listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))),
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
    empty = false,
  )
}

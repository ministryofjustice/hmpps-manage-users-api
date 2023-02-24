package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminTypeReturn
import uk.gov.justice.digital.hmpps.manageusersapi.model.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRoleDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendmentDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendmentDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendmentDto
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.RolesApiService as ExternalRolesApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.RolesApiService as NomisRolesApiService

@Service
class RolesService(
  val nomisRolesApiService: NomisRolesApiService,
  val externalRolesApiService: ExternalRolesApiService
) {

  fun createRole(role: CreateRoleDto) {
    if (role.adminType.hasDPSAdminType()) {
      nomisRolesApiService.createRole(role)
    }
    externalRolesApiService.createRole(role)
  }

  fun getRoles(
    adminTypes: List<AdminType>?
  ): List<Role> = externalRolesApiService.getRoles(adminTypes)

  fun getPagedRoles(
    page: Int,
    size: Int,
    sort: String,
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?
  ) = externalRolesApiService.getPagedRoles(page, size, sort, roleName, roleCode, adminTypes)

  fun getRoleDetail(roleCode: String): Role = externalRolesApiService.getRoleDetail(roleCode)

  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendmentDto) {
    val originalRole = getRoleDetail(roleCode)
    if (originalRole.isDPSRole()) {
      nomisRolesApiService.updateRoleName(roleCode, roleAmendment)
    }
    externalRolesApiService.updateRoleName(roleCode, roleAmendment)
  }

  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendmentDto) =
    externalRolesApiService.updateRoleDescription(roleCode, roleAmendment)

  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendmentDto) {
    val originalRole = externalRolesApiService.getRoleDetail(roleCode)
    if (originalRole.isDpsRoleAdminTypeChanging(roleAmendment.adminType)) {
      nomisRolesApiService.updateRoleAdminType(roleCode, roleAmendment)
    } else if (!originalRole.isDPSRole() && roleAmendment.adminType.hasDPSAdminType()) {
      nomisRolesApiService.createRole(
        CreateRoleDto(
          originalRole.roleCode,
          originalRole.roleName,
          originalRole.roleDescription,
          roleAmendment.adminType
        )
      )
    }
    externalRolesApiService.updateRoleAdminType(roleCode, roleAmendment)
  }

  private fun Role.isDPSRole(): Boolean = adminType.asAdminTypes().hasDPSAdminType()
  private fun Collection<AdminType>.hasDPSAdminType(): Boolean = (DPS_ADM in this) or (DPS_LSA in this)

  private fun Role.isDpsRoleAdminTypeChanging(updatedAdminType: Set<AdminType>): Boolean =
    DPS_LSA !in adminType.asAdminTypes() && DPS_ADM in adminType.asAdminTypes() && DPS_LSA in updatedAdminType ||
      DPS_LSA in adminType.asAdminTypes() && DPS_LSA !in updatedAdminType && DPS_ADM in updatedAdminType
}

fun List<AdminTypeReturn>.asAdminTypes() = map { AdminType.valueOf(it.adminTypeCode) }

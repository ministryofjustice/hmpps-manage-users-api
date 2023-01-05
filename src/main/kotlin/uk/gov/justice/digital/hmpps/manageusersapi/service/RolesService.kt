package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.RolesApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_LSA

@Service
class RolesService(
  val nomisRolesApiService: uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.RolesApiService,
  val externalRolesApiService: RolesApiService
) {

  fun createRole(role: CreateRole) {
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

  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    val originalRole = getRoleDetail(roleCode)
    if (originalRole.isDPSRole()) {
      nomisRolesApiService.updateRoleName(roleCode, roleAmendment)
    }
    externalRolesApiService.updateRoleName(roleCode, roleAmendment)
  }

  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) =
    externalRolesApiService.updateRoleDescription(roleCode, roleAmendment)

  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    val originalRole = externalRolesApiService.getRoleDetail(roleCode)
    if (originalRole.isDpsRoleAdminTypeChanging(roleAmendment.adminType)) {
      nomisRolesApiService.updateRoleAdminType(roleCode, roleAmendment)
    } else if (!originalRole.isDPSRole() && roleAmendment.adminType.hasDPSAdminType()) {
      nomisRolesApiService.createRole(
        CreateRole(
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

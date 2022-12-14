package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RolesPaged
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.ExternalUsersApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.NomisApiService

@Service
class RolesService(
  val nomisApiService: NomisApiService,
  val externalUsersApiService: ExternalUsersApiService
) {

  @Throws(RoleExistsException::class)
  fun createRole(role: CreateRole) {
    if (role.adminType.hasDPSAdminType()) {
      nomisApiService.createRole(role)
    }
    externalUsersApiService.createRole(role)
  }

  fun getRoles(
    adminTypes: List<AdminType>?
  ): List<Role> = externalUsersApiService.getRoles(adminTypes)

  fun getPagedRoles(
    page: Int,
    size: Int,
    sort: String,
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?
  ): RolesPaged =
    externalUsersApiService.getPagedRoles(page, size, sort, roleName, roleCode, adminTypes)

  @Throws(RoleNotFoundException::class)
  fun getRoleDetail(roleCode: String): Role = externalUsersApiService.getRoleDetail(roleCode)

  @Throws(RoleNotFoundException::class)
  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    val originalRole = getRoleDetail(roleCode)
    if (originalRole.isDPSRole()) {
      nomisApiService.updateRoleName(roleCode, roleAmendment)
    }
    externalUsersApiService.updateRoleName(roleCode, roleAmendment)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) =
    externalUsersApiService.updateRoleDescription(roleCode, roleAmendment)

  @Throws(RoleNotFoundException::class)
  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    val originalRole = externalUsersApiService.getRoleDetail(roleCode)
    if (originalRole.isDpsRoleAdminTypeChanging(roleAmendment.adminType)) {
      nomisApiService.updateRoleAdminType(roleCode, roleAmendment)
    } else if (!originalRole.isDPSRole() && roleAmendment.adminType.hasDPSAdminType()) {
      nomisApiService.createRole(
        CreateRole(
          originalRole.roleCode,
          originalRole.roleName,
          originalRole.roleDescription,
          roleAmendment.adminType
        )
      )
    }
    externalUsersApiService.updateRoleAdminType(roleCode, roleAmendment)
  }

  private fun Role.isDPSRole(): Boolean = adminType.asAdminTypes().hasDPSAdminType()
  private fun Collection<AdminType>.hasDPSAdminType(): Boolean = (DPS_ADM in this) or (DPS_LSA in this)

  private fun Role.isDpsRoleAdminTypeChanging(updatedAdminType: Set<AdminType>): Boolean =
    DPS_LSA !in adminType.asAdminTypes() && DPS_ADM in adminType.asAdminTypes() && DPS_LSA in updatedAdminType ||
      DPS_LSA in adminType.asAdminTypes() && DPS_LSA !in updatedAdminType && DPS_ADM in updatedAdminType
}

fun List<AdminTypeReturn>.asAdminTypes() = map { AdminType.valueOf(it.adminTypeCode) }

class RoleExistsException(role: String, errorCode: String) :
  Exception("Unable to create role: $role with reason: $errorCode")

class RoleNotFoundException(action: String, role: String, errorCode: String) :
  Exception("Unable to $action role: $role with reason: $errorCode")

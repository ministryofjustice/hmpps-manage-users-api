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

@Service
class RolesService(
  val authService: AuthService,
  val nomisApiService: NomisApiService,
) {

  @Throws(RoleExistsException::class)
  fun createRole(createRole: CreateRole) {
    // call to hmpps-auth to create the role
    // hmpps-auth called first as it will hold a duplicate copy of the roles in nomis so that we can add a role description
    authService.createRole(createRole)
    // call to Nomis-api to create the new role
    if (createRole.adminType.hasDPSAdminType()) {
      nomisApiService.createRole(createRole)
    }
  }

  fun getAllRoles(page: Int, size: Int, sort: String): RolesPaged {
    return authService.getAllRoles(page, size, sort)
  }

  @Throws(RoleNotFoundException::class)
  fun getRoleDetail(roleCode: String): Role {
    return authService.getRoleDetail(roleCode)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    val originalRole = getRoleDetail(roleCode)
    authService.updateRoleName(roleCode, roleAmendment)
    if (originalRole.isDPSRole()) {
      nomisApiService.updateRoleName(roleCode, roleAmendment)
    }
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) {
    authService.updateRoleDescription(roleCode, roleAmendment)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    val originalRole = authService.getRoleDetail(roleCode)
    authService.updateRoleAdminType(roleCode, roleAmendment)

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
  }

  private fun Role.isDPSRole(): Boolean = adminType.asAdminTypes().hasDPSAdminType()
  private fun Collection<AdminType>.hasDPSAdminType(): Boolean = (DPS_ADM in this) or (DPS_LSA in this)

  private fun List<AdminTypeReturn>.asAdminTypes() = map { AdminType.valueOf(it.adminTypeCode) }

  private fun Role.isDpsRoleAdminTypeChanging(updatedAdminType: Set<AdminType>): Boolean =
    DPS_LSA !in adminType.asAdminTypes() && DPS_ADM in adminType.asAdminTypes() && DPS_LSA in updatedAdminType ||
      DPS_LSA in adminType.asAdminTypes() && DPS_LSA !in updatedAdminType && DPS_ADM in updatedAdminType
}

class RoleExistsException(role: String, errorCode: String) :
  Exception("Unable to create role: $role with reason: $errorCode")

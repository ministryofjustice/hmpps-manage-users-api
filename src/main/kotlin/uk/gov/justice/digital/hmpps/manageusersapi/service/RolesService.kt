package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
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
    if (createRole.adminType.isDPSRole()) {
      nomisApiService.createRole(createRole)
    }
  }

  @Throws(RoleNotFoundException::class)
  fun getRoleDetail(roleCode: String): Role {
    return authService.getRoleDetail(roleCode)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    val originalRole = getRoleDetail(roleCode)
    authService.updateRoleName(roleCode, roleAmendment)
    if (originalRole.adminType.isDPSRole()) {
      nomisApiService.updateRoleName(roleCode, roleAmendment)
    }
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) {
    authService.updateRoleDescription(roleCode, roleAmendment)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    authService.updateRoleAdminType(roleCode, roleAmendment)
  }

  private fun Set<AdminType>.isDPSRole(): Boolean = (DPS_ADM in this) or (DPS_LSA in this)
  private fun List<AdminTypeReturn>.isDPSRole(): Boolean =
    map { it.adminTypeCode }.any { (it == DPS_ADM.adminTypeCode || (it == DPS_LSA.adminTypeCode)) }
}

class RoleExistsException(role: String, errorCode: String) :
  Exception("Unable to create role: $role with reason: $errorCode")

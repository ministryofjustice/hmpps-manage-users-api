package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment

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

  private fun Set<AdminType>.isDPSRole(): Boolean = (AdminType.DPS_ADM in this) or (AdminType.DPS_LSA in this)

  @Throws(RoleNotFoundException::class)
  fun getRoleDetail(roleCode: String): Role {
    return authService.getRoleDetail(roleCode)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    authService.updateRoleName(roleCode, roleAmendment)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) {
    authService.updateRoleDescription(roleCode, roleAmendment)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    authService.updateRoleAdminType(roleCode, roleAmendment)
  }
}

class RoleExistsException(role: String, errorCode: String) :
  Exception("Unable to create role: $role with reason: $errorCode")

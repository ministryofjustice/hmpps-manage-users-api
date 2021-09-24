package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment

@Service
class RolesService(
  val authService: AuthService,
) {

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
}

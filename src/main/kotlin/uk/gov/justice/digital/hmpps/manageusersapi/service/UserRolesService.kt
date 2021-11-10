package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserRoleDetail

@Service
class UserRolesService(
  val authService: AuthService,
  val nomisApiService: NomisApiService,
) {

  fun getUserRoles(user: String): UserRoleDetail {
    val userRoleDetail = nomisApiService.getUserRoles(user)
    val authRoles = authService.getRoles(listOf(AdminType.DPS_ADM))

    return userRoleDetail.copy(dpsRoles = userAuthRoleNames(userRoleDetail.dpsRoles, authRoles))
  }

  private fun userAuthRoleNames(roleDetails: List<RoleDetail>, authRoles: List<Role>): List<RoleDetail> {

    val authRoleMap = authRoles.associate { it.roleCode to it.roleName }
    return roleDetails.map { it.copy(name = authRoleMap[it.code] ?: it.name) }.sortedBy { it.name }
  }
}

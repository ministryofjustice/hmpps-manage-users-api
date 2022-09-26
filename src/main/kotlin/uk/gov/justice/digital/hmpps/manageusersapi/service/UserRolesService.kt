package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserRoleDetail

@Service
class UserRolesService(
  val externalUsersApiService: ExternalUsersApiService,
  val nomisApiService: NomisApiService,
) {

  fun getUserRoles(user: String): UserRoleDetail {
    val userRoleDetail = nomisApiService.getUserRoles(user)
    val externalUserRoles = externalUsersApiService.getRoles(listOf(AdminType.DPS_ADM))

    return userRoleDetail.copy(dpsRoles = userExternalUsersRoleNames(userRoleDetail.dpsRoles, externalUserRoles))
  }

  private fun userExternalUsersRoleNames(roleDetails: List<RoleDetail>, authRoles: List<Role>): List<RoleDetail> {

    val authRoleMap = authRoles.associate { it.roleCode to it.roleName }
    return roleDetails.map { it.copy(name = authRoleMap[it.code] ?: it.name) }.sortedBy { it.name }
  }
}

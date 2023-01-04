package uk.gov.justice.digital.hmpps.manageusersapi.service.nomis

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.RolesApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.RoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserRoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType

@Service("NomisUserRolesService")
class UserRolesService(
  val rolesApiService: RolesApiService,
  val nomisApiService: NomisApiService,
) {

  fun getUserRoles(username: String): UserRoleDetail {
    val userRoleDetail = nomisApiService.getUserRoles(username)
    val externalUserRoles = rolesApiService.getRoles(listOf(AdminType.DPS_ADM))

    return userRoleDetail.copy(dpsRoles = userExternalUsersRoleNames(userRoleDetail.dpsRoles, externalUserRoles))
  }

  private fun userExternalUsersRoleNames(roleDetails: List<RoleDetail>, authRoles: List<Role>): List<RoleDetail> {

    val authRoleMap = authRoles.associate { it.roleCode to it.roleName }
    return roleDetails.map { it.copy(name = authRoleMap[it.code] ?: it.name) }.sortedBy { it.name }
  }
}

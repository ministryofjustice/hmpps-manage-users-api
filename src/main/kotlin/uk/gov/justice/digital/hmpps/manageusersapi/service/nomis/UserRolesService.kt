package uk.gov.justice.digital.hmpps.manageusersapi.service.nomis

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.RolesApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.model.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.RoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserRoleDetail

@Service("NomisUserRolesService")
class UserRolesService(
  val externalRolesApiService: RolesApiService,
  val nomisRolesApiService: uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.RolesApiService,
) {

  fun getUserRoles(username: String): UserRoleDetail {
    val userRoleDetail = nomisRolesApiService.getUserRoles(username)
    val externalUserRoles = externalRolesApiService.getRoles(listOf(AdminType.DPS_ADM))

    return userRoleDetail.copy(dpsRoles = userExternalUsersRoleNames(userRoleDetail.dpsRoles, externalUserRoles))
  }

  private fun userExternalUsersRoleNames(roleDetails: List<RoleDetail>, authRoles: List<Role>): List<RoleDetail> {

    val authRoleMap = authRoles.associate { it.roleCode to it.roleName }
    return roleDetails.map { it.copy(name = authRoleMap[it.code] ?: it.name) }.sortedBy { it.name }
  }
}

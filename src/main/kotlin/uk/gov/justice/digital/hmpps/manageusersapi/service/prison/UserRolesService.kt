package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.RolesApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonRole
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserRole
import uk.gov.justice.digital.hmpps.manageusersapi.model.Role
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.RolesApiService as PrisonRolesApiService

@Service("PrisonUserRolesService")
class UserRolesService(
  val externalRolesApiService: RolesApiService,
  val prisonRolesApiService: PrisonRolesApiService,
) {

  fun getUserRoles(username: String): PrisonUserRole {
    val userRoleDetail = prisonRolesApiService.getUserRoles(username)
    val externalUserRoles = externalRolesApiService.getRoles(listOf(AdminType.DPS_ADM))

    return userRoleDetail.copy(dpsRoles = userExternalUsersRoleNames(userRoleDetail.dpsRoles, externalUserRoles))
  }

  private fun userExternalUsersRoleNames(roleDetails: List<PrisonRole>, authRoles: List<Role>): List<PrisonRole> {
    val authRoleMap = authRoles.associate { it.roleCode to it.roleName }
    return roleDetails.map { it.copy(name = authRoleMap[it.code] ?: it.name) }.sortedBy { it.name }
  }
}

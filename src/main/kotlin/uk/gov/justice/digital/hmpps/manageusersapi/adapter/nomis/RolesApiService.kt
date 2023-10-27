package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRoleDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendmentDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendmentDto

@Service(value = "nomisRolesApiService")
class RolesApiService(
  @Qualifier("nomisUserWebClientUtils") val userWebClientUtils: WebClientUtils,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createRole(createRole: CreateRoleDto) {
    log.debug("Create dps role for {} with {}", createRole.roleCode, createRole)
    return userWebClientUtils.postWithBody(
      mapOf(
        "code" to createRole.roleCode,
        "name" to createRole.roleName.nomisRoleName(),
        "adminRoleOnly" to createRole.adminType.adminRoleOnly(),
      ),
      "/roles",

    )
  }

  fun updateRoleName(roleCode: String, roleNameAmendment: RoleNameAmendmentDto) {
    log.debug("Updating dps role name for {} with {}", roleCode, roleNameAmendment)
    userWebClientUtils.putWithBody(
      mapOf(
        "name" to roleNameAmendment.roleName.nomisRoleName(),
      ),
      "/roles/{roleCode}",
      roleCode,
    )
  }

  fun updateRoleAdminType(roleCode: String, roleAdminTypeAmendment: RoleAdminTypeAmendmentDto) {
    log.debug("Updating dps role name for {} with {}", roleCode, roleAdminTypeAmendment)
    userWebClientUtils.putWithBody(
      mapOf(
        "adminRoleOnly" to roleAdminTypeAmendment.adminType.adminRoleOnly(),
      ),
      "/roles/{roleCode}",
      roleCode,
    )
  }

  fun getUserRoles(username: String) =
    userWebClientUtils.get("/users/{username}/roles", PrisonUserRole::class.java, username)

  private fun String.nomisRoleName(): String = take(30)

  private fun Set<AdminType>.adminRoleOnly(): Boolean = (AdminType.DPS_LSA !in this)
}

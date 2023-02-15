package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserRoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType

@Service(value = "nomisRolesApiService")
class RolesApiService(
  val nomisUserWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createRole(createRole: CreateRole) {
    log.debug("Create dps role for {} with {}", createRole.roleCode, createRole)
    return nomisUserWebClientUtils.post(
      "/roles",
      mapOf(
        "code" to createRole.roleCode,
        "name" to createRole.roleName.nomisRoleName(),
        "adminRoleOnly" to createRole.adminType.adminRoleOnly(),
      )
    )
  }

  fun updateRoleName(roleCode: String, roleNameAmendment: RoleNameAmendment) {
    log.debug("Updating dps role name for {} with {}", roleCode, roleNameAmendment)
    nomisUserWebClientUtils.put(
      "/roles/$roleCode",
      mapOf(
        "name" to roleNameAmendment.roleName.nomisRoleName()
      )
    )
  }

  fun updateRoleAdminType(roleCode: String, roleAdminTypeAmendment: RoleAdminTypeAmendment) {
    log.debug("Updating dps role name for {} with {}", roleCode, roleAdminTypeAmendment)
    nomisUserWebClientUtils.put(
      "/roles/$roleCode",
      mapOf(
        "adminRoleOnly" to roleAdminTypeAmendment.adminType.adminRoleOnly()
      )
    )
  }

  fun getUserRoles(username: String) =
    nomisUserWebClientUtils.get("/users/$username/roles", UserRoleDetail::class.java)

  private fun String.nomisRoleName(): String = take(30)

  private fun Set<AdminType>.adminRoleOnly(): Boolean = (AdminType.DPS_LSA !in this)
}

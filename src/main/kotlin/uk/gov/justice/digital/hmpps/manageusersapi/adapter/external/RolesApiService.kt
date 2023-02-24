package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.model.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRoleDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendmentDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendmentDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendmentDto
import kotlin.collections.ArrayList

@Service(value = "externalRolesApiService")
class RolesApiService(
  @Qualifier("externalUsersUserWebClientUtils") val userWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getRoles(adminTypes: List<AdminType>?): List<Role> =
    userWebClientUtils.getWithParams("/roles", RoleList::class.java, mapOf("adminTypes" to adminTypes as Any?))

  fun getPagedRoles(
    page: Int,
    size: Int,
    sort: String,
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?
  ) = userWebClientUtils.getWithParams(
    "/roles/paged", object : ParameterizedTypeReference<PagedResponse<RoleDto>> () {},
    mapOf(
      "page" to page,
      "size" to size,
      "sort" to sort,
      "roleName" to roleName,
      "roleCode" to roleCode,
      "adminTypes" to adminTypes
    )
  )

  fun getRoleDetail(roleCode: String): Role =
    userWebClientUtils.get("/roles/$roleCode", Role::class.java)

  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendmentDto) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    userWebClientUtils.put("/roles/$roleCode", roleAmendment)
  }

  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendmentDto) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    userWebClientUtils.put("/roles/$roleCode/description", roleAmendment)
  }

  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendmentDto) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    userWebClientUtils.put("/roles/$roleCode/admintype", mapOf("adminType" to roleAmendment.adminType.addDpsAdmTypeIfRequiredAsList()))
  }

  fun createRole(createRole: CreateRoleDto) {
    userWebClientUtils.post(
      "/roles",
      mapOf(
        "roleCode" to createRole.roleCode,
        "roleName" to createRole.roleName,
        "roleDescription" to createRole.roleDescription,
        "adminType" to createRole.adminType.addDpsAdmTypeIfRequiredAsList()
      )
    )
  }
}

private fun Set<AdminType>.addDpsAdmTypeIfRequiredAsList() =
  (if (AdminType.DPS_LSA in this) (this + AdminType.DPS_ADM) else this).map { it.adminTypeCode }

class RoleList : MutableList<Role> by ArrayList()

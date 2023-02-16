package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType
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
    "/roles/paged", object : ParameterizedTypeReference<PagedResponse<Role>> () {},
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

  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    userWebClientUtils.put("/roles/$roleCode", roleAmendment)
  }

  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    userWebClientUtils.put("/roles/$roleCode/description", roleAmendment)
  }

  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    userWebClientUtils.put("/roles/$roleCode/admintype", mapOf("adminType" to roleAmendment.adminType.addDpsAdmTypeIfRequiredAsList()))
  }

  fun createRole(createRole: CreateRole) {
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

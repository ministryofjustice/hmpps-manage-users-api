package uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers

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
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserRole
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.EmailNotificationDto
import java.util.UUID
import kotlin.collections.ArrayList

@Service
class ExternalUsersApiService(
  @Qualifier("externalUsersWebClientUtils") val externalUsersWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getRoles(adminTypes: List<AdminType>?): List<Role> =
    externalUsersWebClientUtils.getWithParams("/roles", RoleList::class.java, mapOf("adminTypes" to adminTypes as Any?))

  fun getPagedRoles(
    page: Int,
    size: Int,
    sort: String,
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?
  ) = externalUsersWebClientUtils.getWithParams(
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
    externalUsersWebClientUtils.get("/roles/$roleCode", Role::class.java)

  @Throws(RoleNotFoundException::class)
  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    externalUsersWebClientUtils.put("/roles/$roleCode", roleAmendment)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    externalUsersWebClientUtils.put("/roles/$roleCode/description", roleAmendment)
  }

  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    externalUsersWebClientUtils.put("/roles/$roleCode/admintype", mapOf("adminType" to roleAmendment.adminType.addDpsAdmTypeIfRequiredAsList()))
  }

  fun createRole(createRole: CreateRole) {
    externalUsersWebClientUtils.post(
      "/roles",
      mapOf(
        "roleCode" to createRole.roleCode,
        "roleName" to createRole.roleName,
        "roleDescription" to createRole.roleDescription,
        "adminType" to createRole.adminType.addDpsAdmTypeIfRequiredAsList()
      )
    )
  }

  fun validateEmailDomain(emailDomain: String) =
    externalUsersWebClientUtils.get("/validate/email-domain?emailDomain=$emailDomain", Boolean::class.java)

  fun enableUserById(userId: UUID): EmailNotificationDto {
    log.debug("Enabling User for User Id of {} ", userId)
    return externalUsersWebClientUtils.putWithResponse("/users/$userId/enable", EmailNotificationDto::class.java)
  }

  fun disableUserById(userId: UUID, deactivateReason: DeactivateReason) {
    log.debug("Disabling User for User Id of {} ", userId)
    externalUsersWebClientUtils.put("/users/$userId/disable", deactivateReason)
  }
}

private fun Set<AdminType>.addDpsAdmTypeIfRequiredAsList() =
  (if (AdminType.DPS_LSA in this) (this + AdminType.DPS_ADM) else this).map { it.adminTypeCode }

class UserRoleList : MutableList<UserRole> by ArrayList()
class RoleList : MutableList<Role> by ArrayList()
class GroupList : MutableList<UserGroup> by ArrayList()
class UserList : MutableList<ExternalUserDetailsDto> by ArrayList()

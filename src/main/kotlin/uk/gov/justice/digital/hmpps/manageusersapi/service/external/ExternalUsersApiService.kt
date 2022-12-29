package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ChildGroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateChildGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserRole
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleNotFoundException
import java.util.UUID
import kotlin.collections.ArrayList

@Service
class ExternalUsersApiService(
  @Qualifier("externalUsersWebClient") val externalUsersWebClient: WebClient,
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

  fun getUserRoles(userId: UUID): List<UserRole> =
    externalUsersWebClientUtils.get("/users/$userId/roles", UserRoleList::class.java)

  fun addRolesByUserId(userId: UUID, roleCodes: List<String>) {
    log.debug("Adding roles {} for user {}", roleCodes, userId)
    externalUsersWebClientUtils.post("/users/$userId/roles", roleCodes)
  }

  fun deleteRoleByUserId(userId: UUID, role: String) {
    log.debug("Delete role {} for user {}", role, userId)
    externalUsersWebClient.delete()
      .uri("/users/$userId/roles/$role")
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun getAssignableRoles(userId: UUID) =
    externalUsersWebClientUtils.get("/users/$userId/assignable-roles", UserRoleList::class.java)

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

  fun getGroups(): List<UserGroup> =
    externalUsersWebClientUtils.get("/groups", GroupList::class.java)

  fun getGroupDetail(group: String): GroupDetails =
    externalUsersWebClientUtils.get("/groups/$group", GroupDetails::class.java)

  fun getChildGroupDetail(group: String): ChildGroupDetails =
    externalUsersWebClientUtils.get("/groups/child/$group", ChildGroupDetails::class.java)

  fun updateGroup(group: String, groupAmendment: GroupAmendment) {
    log.debug("Updating group details for {} with {}", group, groupAmendment)
    externalUsersWebClientUtils.put("/groups/$group", groupAmendment)
  }

  fun updateChildGroup(group: String, groupAmendment: GroupAmendment) {
    log.debug("Updating child group details for {} with {}", group, groupAmendment)
    externalUsersWebClientUtils.put("/groups/child/$group", groupAmendment)
  }

  fun createGroup(createGroup: CreateGroup) {
    externalUsersWebClientUtils.post(
      "/groups",
      mapOf(
        "groupCode" to createGroup.groupCode,
        "groupName" to createGroup.groupName
      )
    )
  }

  fun createChildGroup(createChildGroup: CreateChildGroup) {
    externalUsersWebClientUtils.post(
      "/groups/child",
      mapOf(
        "groupCode" to createChildGroup.groupCode,
        "groupName" to createChildGroup.groupName,
        "parentGroupCode" to createChildGroup.parentGroupCode
      )
    )
  }

  fun deleteGroupByUserId(userId: UUID, group: String) {
    log.debug("Delete group {} for user {}", group, userId)
    externalUsersWebClient.delete()
      .uri("/users/$userId/groups/$group")
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun deleteChildGroup(group: String) {
    log.debug("Deleting child group {}", group)
    externalUsersWebClient.delete()
      .uri("/groups/child/$group")
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun deleteGroup(group: String) {
    externalUsersWebClient.delete()
      .uri("/groups/$group")
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun validateEmailDomain(emailDomain: String) =
    externalUsersWebClientUtils.get("/validate/email-domain?emailDomain=$emailDomain", Boolean::class.java)

  fun getUserGroups(userId: UUID, children: Boolean): List<UserGroup> =
    externalUsersWebClientUtils.getWithParams("/users/$userId/groups", GroupList::class.java, mapOf("children" to children))

  fun addGroupByUserId(userId: UUID, group: String) {
    log.debug("Adding group {} for user {}", group, userId)
    externalUsersWebClientUtils.put("/users/$userId/groups/$group")
  }

  fun enableUserById(userId: UUID): EmailNotificationDto {
    log.debug("Enabling User for User Id of {} ", userId)
    return externalUsersWebClientUtils.putWithResponse("/users/$userId/enable", EmailNotificationDto::class.java)
  }

  fun disableUserById(userId: UUID, deactivateReason: DeactivateReason) {
    log.debug("Disabling User for User Id of {} ", userId)
    externalUsersWebClientUtils.put("/users/$userId/disable", deactivateReason)
  }

  fun findUsersByEmail(email: String): List<ExternalUserDetailsDto>? =
    externalUsersWebClientUtils.getIfPresent("/users?email=$email", UserList::class.java)

  fun findUsers(
    name: String?,
    roles: List<String>?,
    groups: List<String>?,
    pageable: Pageable,
    status: Status
  ) =
    externalUsersWebClientUtils.getWithParams(
      "/users/search", object : ParameterizedTypeReference<PagedResponse<ExternalUserDetailsDto>> () {},
      mapOf(
        "name" to name,
        "roles" to roles?.joinToString(","),
        "groups" to groups?.joinToString(","),
        "status" to status,
        "page" to pageable.pageNumber,
        "size" to pageable.pageSize
      )
    )

  fun findUserByUsername(userName: String): ExternalUserDetailsDto? =
    externalUsersWebClientUtils.getIfPresent("/users/$userName", ExternalUserDetailsDto::class.java)

  fun getMyAssignableGroups(): List<UserGroup> =
    externalUsersWebClientUtils.get("/users/me/assignable-groups", GroupList::class.java)
}

private fun Set<AdminType>.addDpsAdmTypeIfRequiredAsList() =
  (if (AdminType.DPS_LSA in this) (this + AdminType.DPS_ADM) else this).map { it.adminTypeCode }

class UserRoleList : MutableList<UserRole> by ArrayList()
class RoleList : MutableList<Role> by ArrayList()
class GroupList : MutableList<UserGroup> by ArrayList()
class UserList : MutableList<ExternalUserDetailsDto> by ArrayList()

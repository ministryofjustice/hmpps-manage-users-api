package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageusersapi.resource.ChildGroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateChildGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RolesPaged
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserGroup
import java.util.UUID

@Service
class ExternalUsersApiService(
  @Qualifier("externalUsersWebClient") val externalUsersWebClient: WebClient
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getRoles(adminTypes: List<AdminType>?): List<Role> =
    externalUsersWebClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/roles")
          .queryParam("adminTypes", adminTypes)
          .build()
      }
      .retrieve()
      .bodyToMono(RoleList::class.java)
      .block()!!

  fun getPagedRoles(
    page: Int,
    size: Int,
    sort: String,
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?
  ): RolesPaged =
    externalUsersWebClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/roles/paged")
          .queryParam("page", page)
          .queryParam("size", size)
          .queryParam("sort", sort)
          .queryParam("roleName", roleName)
          .queryParam("roleCode", roleCode)
          .queryParam("adminTypes", adminTypes)
          .build()
      }
      .retrieve()
      .bodyToMono(RolesPaged::class.java)
      .block()!!

  fun getRoleDetail(roleCode: String): Role =
    externalUsersWebClient.get()
      .uri("/roles/$roleCode")
      .retrieve()
      .bodyToMono(Role::class.java)
      .block() ?: throw RoleNotFoundException("get", roleCode, "notfound")

  @Throws(RoleNotFoundException::class)
  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    externalUsersWebClient.put()
      .uri("/roles/$roleCode")
      .bodyValue(roleAmendment)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    externalUsersWebClient.put()
      .uri("/roles/$roleCode/description")
      .bodyValue(roleAmendment)
      .retrieve()
      .toBodilessEntity()
      .block()
  }
  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    externalUsersWebClient.put()
      .uri("/roles/$roleCode/admintype")
      .bodyValue(mapOf("adminType" to roleAmendment.adminType.addDpsAdmTypeIfRequiredAsList()))
      .retrieve()
      .toBodilessEntity()
      .block() ?: throw RuntimeException("Failed to update role")
  }

  fun createRole(createRole: CreateRole) {
    externalUsersWebClient.post()
      .uri("/roles")
      .bodyValue(
        mapOf(
          "roleCode" to createRole.roleCode,
          "roleName" to createRole.roleName,
          "roleDescription" to createRole.roleDescription,
          "adminType" to createRole.adminType.addDpsAdmTypeIfRequiredAsList(),
        )
      )
      .retrieve()
      .toBodilessEntity()
      .block() ?: throw RuntimeException("Failed to create role")
  }

  fun getGroups(): List<UserGroup> =
    externalUsersWebClient.get()
      .uri("/groups")
      .retrieve()
      .bodyToMono(GroupList::class.java)
      .block() ?: throw RuntimeException("Failed to retrieve Groups")

  fun getGroupDetail(group: String): GroupDetails =
    externalUsersWebClient.get()
      .uri("/groups/$group")
      .retrieve()
      .bodyToMono(GroupDetails::class.java)
      .block() ?: throw RuntimeException("Failed to retrieve group details")

  fun getChildGroupDetail(group: String): ChildGroupDetails =
    externalUsersWebClient.get()
      .uri("/groups/child/$group")
      .retrieve()
      .bodyToMono(ChildGroupDetails::class.java)
      .block() ?: throw RuntimeException("Failed to retrieve child group details")

  fun updateGroup(group: String, groupAmendment: GroupAmendment) {
    log.debug("Updating group details for {} with {}", group, groupAmendment)
    externalUsersWebClient.put()
      .uri("/groups/$group")
      .bodyValue(groupAmendment)
      .retrieve()
      .toBodilessEntity()
      .block() ?: throw RuntimeException("Failed to update group details")
  }

  fun updateChildGroup(group: String, groupAmendment: GroupAmendment) {

    log.debug("Updating child group details for {} with {}", group, groupAmendment)
    externalUsersWebClient.put()
      .uri("/groups/child/$group")
      .bodyValue(groupAmendment)
      .retrieve()
      .toBodilessEntity()
      .block() ?: throw RuntimeException("Failed to update child group details")
  }

  fun createGroup(createGroup: CreateGroup) {
    externalUsersWebClient.post()
      .uri("/groups")
      .bodyValue(
        mapOf(
          "groupCode" to createGroup.groupCode,
          "groupName" to createGroup.groupName
        )
      )
      .retrieve()
      .toBodilessEntity()
      .block() ?: throw RuntimeException("Failed to create group")
  }

  fun createChildGroup(createChildGroup: CreateChildGroup) {
    externalUsersWebClient.post()
      .uri("/groups/child")
      .bodyValue(
        mapOf(
          "groupCode" to createChildGroup.groupCode,
          "groupName" to createChildGroup.groupName,
          "parentGroupCode" to createChildGroup.parentGroupCode
        )
      )
      .retrieve()
      .toBodilessEntity()
      .block() ?: throw RuntimeException("Failed to create child group details")
  }

  fun deleteChildGroup(group: String) {
    log.debug("Deleting child group {}", group)
    externalUsersWebClient.delete()
      .uri("/groups/child/$group")
      .retrieve()
      .toBodilessEntity()
      .block() ?: throw RuntimeException("Failed to delete child group")
  }

  fun deleteGroup(group: String) {
    externalUsersWebClient.delete()
      .uri("/groups/$group")
      .retrieve()
      .toBodilessEntity()
      .block() ?: throw RuntimeException("Failed to delete group")
  }

  fun validateEmailDomain(emailDomain: String): Boolean {
    return externalUsersWebClient.get()
      .uri("/validate/email-domain?emailDomain=$emailDomain")
      .retrieve()
      .bodyToMono(Boolean::class.java)
      .block() ?: throw RuntimeException("Failed to validate email domain")
  }

  fun getUserGroups(userId: UUID, children: Boolean): List<UserGroup> =
    externalUsersWebClient.get().uri {
      it.path("/users/id/$userId/groups")
        .queryParam("children", children)
        .build()
    }
      .retrieve()
      .bodyToMono(GroupList::class.java)
      .block() ?: throw RuntimeException("Failed to retrieve user groups")
}

private fun Set<AdminType>.addDpsAdmTypeIfRequiredAsList() =
  (if (AdminType.DPS_LSA in this) (this + AdminType.DPS_ADM) else this).map { it.adminTypeCode }

class RoleList : MutableList<Role> by ArrayList()
class GroupList : MutableList<UserGroup> by ArrayList()

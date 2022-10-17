package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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
import java.time.Duration
import java.util.UUID
import kotlin.collections.ArrayList

@Service
class ExternalUsersApiService(
  @Qualifier("externalUsersWebClient") val externalUsersWebClient: WebClient,
  @Value("\${api.timeout:10s}")
  val timeout: Duration
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

  @Throws(RoleNotFoundException::class)
  fun getRoleDetail(roleCode: String): Role =
    externalUsersWebClient.get()
      .uri("/roles/$roleCode")
      .retrieve()
      .bodyToMono(Role::class.java)
      .block(timeout) ?: throw RoleNotFoundException("get", roleCode, "notfound")

  @Throws(RoleNotFoundException::class)
  fun updateRoleName(roleCode: String, roleAmendment: RoleNameAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    externalUsersWebClient.put()
      .uri("/roles/$roleCode")
      .bodyValue(roleAmendment)
      .retrieve()
      .toBodilessEntity()
      .block(timeout)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleDescription(roleCode: String, roleAmendment: RoleDescriptionAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    externalUsersWebClient.put()
      .uri("/roles/$roleCode/description")
      .bodyValue(roleAmendment)
      .retrieve()
      .toBodilessEntity()
      .block(timeout)
  }

  @Throws(RoleNotFoundException::class)
  fun updateRoleAdminType(roleCode: String, roleAmendment: RoleAdminTypeAmendment) {
    log.debug("Updating role for {} with {}", roleCode, roleAmendment)
    externalUsersWebClient.put()
      .uri("/roles/$roleCode/admintype")
      .bodyValue(mapOf("adminType" to roleAmendment.adminType.addDpsAdmTypeIfRequiredAsList()))
      .retrieve()
      .toBodilessEntity()
      .block(timeout)
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
      .block(timeout)
  }

  fun getGroups(): List<UserGroup> =
    externalUsersWebClient.get()
      .uri("/groups")
      .retrieve()
      .bodyToMono(GroupList::class.java)
      .block(timeout)

  fun getGroupDetail(group: String): GroupDetails =
    externalUsersWebClient.get()
      .uri("/groups/$group")
      .retrieve()
      .bodyToMono(GroupDetails::class.java)
      .block(timeout) ?: throw GroupNotFoundException("get", group, "notfound")

  fun getChildGroupDetail(group: String): ChildGroupDetails =
    externalUsersWebClient.get()
      .uri("/groups/child/$group")
      .retrieve()
      .bodyToMono(ChildGroupDetails::class.java)
      .block(timeout) ?: throw RuntimeException("Failed to retrieve child group details")

  fun updateGroup(group: String, groupAmendment: GroupAmendment) {
    log.debug("Updating group details for {} with {}", group, groupAmendment)
    externalUsersWebClient.put()
      .uri("/groups/$group")
      .bodyValue(groupAmendment)
      .retrieve()
      .toBodilessEntity()
      .block(timeout)
  }

  fun updateChildGroup(group: String, groupAmendment: GroupAmendment) {

    log.debug("Updating child group details for {} with {}", group, groupAmendment)
    externalUsersWebClient.put()
      .uri("/groups/child/$group")
      .bodyValue(groupAmendment)
      .retrieve()
      .toBodilessEntity()
      .block(timeout) ?: throw ChildGroupNotFoundException(group, "notfound")
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
      .block(timeout)
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
      .block(timeout)
  }

  fun deleteGroupByUserId(userId: UUID, group: String) {
    log.debug("Delete group {} for user {}", group, userId)
    externalUsersWebClient.delete()
      .uri("/users/id/$userId/groups/$group")
      .retrieve()
      .toBodilessEntity()
      .block(timeout)
  }

  fun deleteChildGroup(group: String) {
    log.debug("Deleting child group {}", group)
    externalUsersWebClient.delete()
      .uri("/groups/child/$group")
      .retrieve()
      .toBodilessEntity()
      .block(timeout)
  }

  fun validateEmailDomain(emailDomain: String): Boolean {
    return externalUsersWebClient.get()
      .uri("/validate/email-domain?emailDomain=$emailDomain")
      .retrieve()
      .bodyToMono(Boolean::class.java)
      .block(timeout)!!
  }

  fun deleteGroup(group: String) {
    externalUsersWebClient.delete()
      .uri("/groups/$group")
      .retrieve()
      .toBodilessEntity()
      .block(timeout)
  }
}

private fun Set<AdminType>.addDpsAdmTypeIfRequiredAsList() =
  (if (AdminType.DPS_LSA in this) (this + AdminType.DPS_ADM) else this).map { it.adminTypeCode }

class RoleList : MutableList<Role> by ArrayList()
class GroupList : MutableList<UserGroup> by ArrayList()

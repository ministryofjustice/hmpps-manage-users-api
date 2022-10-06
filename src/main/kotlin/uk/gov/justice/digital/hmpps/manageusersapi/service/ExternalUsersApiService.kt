package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleAdminTypeAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDescriptionAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RolesPaged
import java.time.Duration

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
    AuthService.log.debug("Updating role for {} with {}", roleCode, roleAmendment)
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

  fun getGroupDetail(group: String): GroupDetails =
    externalUsersWebClient.get()
      .uri("/groups/$group")
      .retrieve()
      .bodyToMono(GroupDetails::class.java)
      .block(timeout) ?: throw GroupNotFoundException("get", group, "notfound")
  fun updateChildGroup(group: String, groupAmendment: GroupAmendment) {

    log.debug("Updating child group details for {} with {}", group, groupAmendment)
    externalUsersWebClient.put()
      .uri("/groups/child/$group")
      .bodyValue(groupAmendment)
      .retrieve()
      .toBodilessEntity()
      .block(timeout) ?: throw ChildGroupNotFoundException(group, "notfound")
  }
}

private fun Set<AdminType>.addDpsAdmTypeIfRequiredAsList() =
  (if (AdminType.DPS_LSA in this) (this + AdminType.DPS_ADM) else this).map { it.adminTypeCode }

class RoleList : MutableList<Role> by ArrayList()

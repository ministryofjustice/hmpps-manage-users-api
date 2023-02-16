package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ChildGroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateChildGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserGroup

@Service
class GroupsApiService(
  @Qualifier("externalUsersUserWebClientUtils") val userWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getGroups(): List<UserGroup> =
    userWebClientUtils.get("/groups", GroupList::class.java)

  fun getGroupDetail(group: String): GroupDetails =
    userWebClientUtils.get("/groups/$group", GroupDetails::class.java)

  fun getChildGroupDetail(group: String): ChildGroupDetails =
    userWebClientUtils.get("/groups/child/$group", ChildGroupDetails::class.java)

  fun updateGroup(group: String, groupAmendment: GroupAmendment) {
    log.debug("Updating group details for {} with {}", group, groupAmendment)
    userWebClientUtils.put("/groups/$group", groupAmendment)
  }

  fun updateChildGroup(group: String, groupAmendment: GroupAmendment) {
    log.debug("Updating child group details for {} with {}", group, groupAmendment)
    userWebClientUtils.put("/groups/child/$group", groupAmendment)
  }

  fun createGroup(createGroup: CreateGroup) {
    userWebClientUtils.post(
      "/groups",
      mapOf(
        "groupCode" to createGroup.groupCode,
        "groupName" to createGroup.groupName
      )
    )
  }

  fun createChildGroup(createChildGroup: CreateChildGroup) {
    userWebClientUtils.post(
      "/groups/child",
      mapOf(
        "groupCode" to createChildGroup.groupCode,
        "groupName" to createChildGroup.groupName,
        "parentGroupCode" to createChildGroup.parentGroupCode
      )
    )
  }

  fun deleteChildGroup(group: String) {
    log.debug("Deleting child group {}", group)
    userWebClientUtils.delete("/groups/child/$group")
  }

  fun deleteGroup(group: String) {
    userWebClientUtils.delete("/groups/$group")
  }
}

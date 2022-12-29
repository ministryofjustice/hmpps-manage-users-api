package uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers

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
  @Qualifier("externalUsersWebClientUtils") val externalUsersWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
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

  fun deleteChildGroup(group: String) {
    log.debug("Deleting child group {}", group)
    externalUsersWebClientUtils.delete("/groups/child/$group")
  }

  fun deleteGroup(group: String) {
    externalUsersWebClientUtils.delete("/groups/$group")
  }
}

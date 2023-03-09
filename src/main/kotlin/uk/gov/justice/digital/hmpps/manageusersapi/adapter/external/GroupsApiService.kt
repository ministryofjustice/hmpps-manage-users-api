package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.manageusersapi.model.Group
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateChildGroupDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateGroupDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupAmendmentDto

@Service
class GroupsApiService(
  @Qualifier("externalUsersUserWebClientUtils") val userWebClientUtils: WebClientUtils,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getGroups(): List<UserGroup> =
    userWebClientUtils.get("/groups", GroupList::class.java)

  fun getGroupDetail(group: String): Group =
    userWebClientUtils.get("/groups/$group", Group::class.java)

  fun getChildGroupDetail(group: String): ChildGroup =
    userWebClientUtils.get("/groups/child/$group", ChildGroup::class.java)

  fun updateGroup(group: String, groupAmendment: GroupAmendmentDto) {
    log.debug("Updating group details for {} with {}", group, groupAmendment)
    userWebClientUtils.put("/groups/$group", groupAmendment)
  }

  fun updateChildGroup(group: String, groupAmendment: GroupAmendmentDto) {
    log.debug("Updating child group details for {} with {}", group, groupAmendment)
    userWebClientUtils.put("/groups/child/$group", groupAmendment)
  }

  fun createGroup(createGroup: CreateGroupDto) {
    userWebClientUtils.post(
      "/groups",
      mapOf(
        "groupCode" to createGroup.groupCode,
        "groupName" to createGroup.groupName,
      ),
    )
  }

  fun createChildGroup(createChildGroup: CreateChildGroupDto) {
    userWebClientUtils.post(
      "/groups/child",
      mapOf(
        "groupCode" to createChildGroup.groupCode,
        "groupName" to createChildGroup.groupName,
        "parentGroupCode" to createChildGroup.parentGroupCode,
      ),
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

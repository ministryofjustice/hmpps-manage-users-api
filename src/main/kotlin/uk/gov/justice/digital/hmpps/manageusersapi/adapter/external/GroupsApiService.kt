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

  fun getGroups(): List<UserGroup> = userWebClientUtils.get("/groups", GroupList::class.java)

  fun getCRSGroups(): List<UserGroup> = userWebClientUtils.get("/groups/subset/crs", GroupList::class.java)

  fun getGroupDetail(group: String): Group = userWebClientUtils.get("/groups/{group}", Group::class.java, group)

  fun getChildGroupDetail(group: String): ChildGroup = userWebClientUtils.get("/groups/child/{group}", ChildGroup::class.java, group)

  fun updateGroup(group: String, groupAmendment: GroupAmendmentDto) {
    log.debug("Updating group details for {} with {}", group, groupAmendment)
    userWebClientUtils.putWithBody(groupAmendment, "/groups/{group}", group)
  }

  fun updateChildGroup(group: String, groupAmendment: GroupAmendmentDto) {
    log.debug("Updating child group details for {} with {}", group, groupAmendment)
    userWebClientUtils.putWithBody(groupAmendment, "/groups/child/{group}", group)
  }

  fun createGroup(createGroup: CreateGroupDto) {
    userWebClientUtils.postWithBody(
      mapOf(
        "groupCode" to createGroup.groupCode,
        "groupName" to createGroup.groupName,
      ),
      "/groups",

    )
  }

  fun createChildGroup(createChildGroup: CreateChildGroupDto) {
    userWebClientUtils.postWithBody(
      mapOf(
        "groupCode" to createChildGroup.groupCode,
        "groupName" to createChildGroup.groupName,
        "parentGroupCode" to createChildGroup.parentGroupCode,
      ),
      "/groups/child",

    )
  }

  fun deleteChildGroup(group: String) {
    log.debug("Deleting child group {}", group)
    userWebClientUtils.delete("/groups/child/{group}", group)
  }

  fun deleteGroup(group: String) {
    userWebClientUtils.delete("/groups/{group}", group)
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.GroupsApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.manageusersapi.model.Group
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateChildGroupDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateGroupDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupAmendmentDto

@Service
class GroupsService(
  val groupsApiService: GroupsApiService,
) {

  fun getGroups(): List<UserGroup> = groupsApiService.getGroups()

  fun getCRSGroups(): List<UserGroup> = groupsApiService.getCRSGroups()

  fun getGroupDetail(
    group: String,
  ): Group = groupsApiService.getGroupDetail(group)

  fun getChildGroupDetail(
    group: String,
  ): ChildGroup = groupsApiService.getChildGroupDetail(group)

  fun updateGroup(groupCode: String, groupAmendment: GroupAmendmentDto) = groupsApiService.updateGroup(groupCode, groupAmendment)

  fun updateChildGroup(groupCode: String, groupAmendment: GroupAmendmentDto) = groupsApiService.updateChildGroup(groupCode, groupAmendment)

  fun createGroup(createGroup: CreateGroupDto) = groupsApiService.createGroup(createGroup)

  fun createChildGroup(createChildGroup: CreateChildGroupDto) = groupsApiService.createChildGroup(createChildGroup)

  fun deleteChildGroup(group: String) = groupsApiService.deleteChildGroup(group)

  fun deleteGroup(group: String) = groupsApiService.deleteGroup(group)
}

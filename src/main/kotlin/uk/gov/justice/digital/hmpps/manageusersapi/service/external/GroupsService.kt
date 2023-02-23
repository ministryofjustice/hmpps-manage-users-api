package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.GroupsApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ChildGroupDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateChildGroupDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateGroupDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupAmendmentDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupDetailsDto

@Service
class GroupsService(
  val groupsApiService: GroupsApiService
) {

  fun getGroups(): List<UserGroup> = groupsApiService.getGroups()

  fun getGroupDetail(
    group: String
  ): GroupDetailsDto = groupsApiService.getGroupDetail(group)

  fun getChildGroupDetail(
    group: String
  ): ChildGroupDetailsDto = groupsApiService.getChildGroupDetail(group)

  fun updateGroup(groupCode: String, groupAmendment: GroupAmendmentDto) = groupsApiService.updateGroup(groupCode, groupAmendment)

  fun updateChildGroup(groupCode: String, groupAmendment: GroupAmendmentDto) = groupsApiService.updateChildGroup(groupCode, groupAmendment)

  fun createGroup(createGroup: CreateGroupDto) = groupsApiService.createGroup(createGroup)

  fun createChildGroup(createChildGroup: CreateChildGroupDto) = groupsApiService.createChildGroup(createChildGroup)

  fun deleteChildGroup(group: String) = groupsApiService.deleteChildGroup(group)

  fun deleteGroup(group: String) = groupsApiService.deleteGroup(group)
}

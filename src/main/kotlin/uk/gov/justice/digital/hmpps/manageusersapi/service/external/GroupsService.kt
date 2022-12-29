package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers.GroupsApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ChildGroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateChildGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserGroup

@Service
class GroupsService(
  val groupsApiService: GroupsApiService
) {

  fun getGroups(): List<UserGroup> = groupsApiService.getGroups()
  fun getGroupDetail(
    group: String
  ): GroupDetails = groupsApiService.getGroupDetail(group)

  fun getChildGroupDetail(
    group: String
  ): ChildGroupDetails = groupsApiService.getChildGroupDetail(group)

  fun updateGroup(groupCode: String, groupAmendment: GroupAmendment) = groupsApiService.updateGroup(groupCode, groupAmendment)

  fun updateChildGroup(groupCode: String, groupAmendment: GroupAmendment) = groupsApiService.updateChildGroup(groupCode, groupAmendment)

  fun createGroup(createGroup: CreateGroup) = groupsApiService.createGroup(createGroup)

  fun createChildGroup(createChildGroup: CreateChildGroup) = groupsApiService.createChildGroup(createChildGroup)

  fun deleteChildGroup(group: String) = groupsApiService.deleteChildGroup(group)

  fun deleteGroup(group: String) = groupsApiService.deleteGroup(group)
}

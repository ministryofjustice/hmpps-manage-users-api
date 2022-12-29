package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers.ExternalUsersApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ChildGroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateChildGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserGroup

@Service
class GroupsService(
  val externalUsersApiService: ExternalUsersApiService
) {

  fun getGroups(): List<UserGroup> = externalUsersApiService.getGroups()
  fun getGroupDetail(
    group: String
  ): GroupDetails = externalUsersApiService.getGroupDetail(group)

  fun getChildGroupDetail(
    group: String
  ): ChildGroupDetails = externalUsersApiService.getChildGroupDetail(group)

  fun updateGroup(groupCode: String, groupAmendment: GroupAmendment) = externalUsersApiService.updateGroup(groupCode, groupAmendment)

  fun updateChildGroup(groupCode: String, groupAmendment: GroupAmendment) = externalUsersApiService.updateChildGroup(groupCode, groupAmendment)

  fun createGroup(createGroup: CreateGroup) = externalUsersApiService.createGroup(createGroup)

  fun createChildGroup(createChildGroup: CreateChildGroup) = externalUsersApiService.createChildGroup(createChildGroup)

  fun deleteChildGroup(group: String) = externalUsersApiService.deleteChildGroup(group)

  fun deleteGroup(group: String) = externalUsersApiService.deleteGroup(group)
}

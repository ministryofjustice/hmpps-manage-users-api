package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateChildGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserGroup

@Service
class GroupsService(
  val externalUsersApiService: ExternalUsersApiService
) {

  fun getGroups(): List<UserGroup> = externalUsersApiService.getGroups()

  @Throws(GroupNotFoundException::class)
  fun getGroupDetail(
    group: String
  ): GroupDetails = externalUsersApiService.getGroupDetail(group)

  fun updateGroup(groupCode: String, groupAmendment: GroupAmendment) = externalUsersApiService.updateGroup(groupCode, groupAmendment)

  @Throws(ChildGroupNotFoundException::class)
  fun updateChildGroup(groupCode: String, groupAmendment: GroupAmendment) = externalUsersApiService.updateChildGroup(groupCode, groupAmendment)

  fun createGroup(createGroup: CreateGroup) = externalUsersApiService.createGroup(createGroup)

  fun createChildGroup(createChildGroup: CreateChildGroup) = externalUsersApiService.createChildGroup(createChildGroup)

  fun deleteChildGroup(group: String) = externalUsersApiService.deleteChildGroup(group)

  fun deleteGroup(group: String) = externalUsersApiService.deleteGroup(group)
}

class ChildGroupNotFoundException(group: String, errorCode: String) :
  Exception("Unable to maintain child group: $group with reason: $errorCode")

class GroupNotFoundException(action: String, group: String, errorCode: String) :
  Exception("Unable to $action group: $group with reason: $errorCode")
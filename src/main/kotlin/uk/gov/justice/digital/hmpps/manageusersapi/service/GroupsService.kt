package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupDetails

@Service
class GroupsService(
  val externalUsersApiService: ExternalUsersApiService
) {
  @Throws(GroupNotFoundException::class)
  fun getGroupDetail(
    group: String
  ): GroupDetails = externalUsersApiService.getGroupDetail(group)

  @Throws(ChildGroupNotFoundException::class)
  fun updateChildGroup(groupCode: String, groupAmendment: GroupAmendment) = externalUsersApiService.updateChildGroup(groupCode, groupAmendment)
  fun createGroup(createGroup: CreateGroup) = externalUsersApiService.createGroup(createGroup)
}

class ChildGroupNotFoundException(val group: String, val errorCode: String) :
  Exception("Unable to maintain child group: $group with reason: $errorCode")

class GroupNotFoundException(val action: String, val group: String, val errorCode: String) :
  Exception("Unable to $action group: $group with reason: $errorCode")
class GroupExistsException(val group: String, val errorCode: String) :
  Exception("Unable to create group: $group with reason: $errorCode")

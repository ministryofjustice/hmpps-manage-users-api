package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserGroupApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserGroupDto
import java.util.UUID

@Service
class UserGroupService(
  val userGroupApiService: UserGroupApiService
) {
  fun removeGroupByUserId(userId: UUID, group: String) = userGroupApiService.deleteGroupByUserId(userId, group)
  fun addGroupByUserId(userId: UUID, group: String) = userGroupApiService.addGroupByUserId(userId, group)

  fun getUserGroups(user: UUID, children: Boolean): List<UserGroupDto> = userGroupApiService.getUserGroups(user, children)

  fun getMyAssignableGroups(): List<UserGroupDto> = userGroupApiService.getMyAssignableGroups()
}

package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserGroupService(
  val externalUsersApiService: ExternalUsersApiService
) {
  fun removeGroupByUserId(userId: UUID, group: String) {
    externalUsersApiService.deleteGroupByUserId(userId, group)
  }
}

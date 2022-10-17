package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserGroup
import java.util.UUID

@Service
class UserGroupService(
  val externalUsersApiService: ExternalUsersApiService
) {
  fun getUserGroups(user: UUID, children: Boolean): List<UserGroup> = externalUsersApiService.getUserGroups(user, children)
}

package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupDetails

@Service
class GroupsService(
  val authService: AuthService,
) {
  @Throws(GroupNotFoundException::class)
  fun getGroupDetail(
    group: String
  ): GroupDetails = authService.getGroupDetail(group)
}

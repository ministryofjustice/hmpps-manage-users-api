package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.Status
import java.util.UUID

@Service("ExternalUserSearchService")
class UserSearchService(
  val userSearchApiService: UserSearchApiService,
) {

  fun findExternalUsersByCrsGroup(crsgroupcode: String) = userSearchApiService.findUsersByCrsGroup(crsgroupcode)

  fun findExternalUsersByEmail(email: String?) = email ?.let { userSearchApiService.findUsersByEmail(email) }

  fun findExternalUserById(userId: UUID) = userSearchApiService.findByUserId(userId)

  fun findExternalUserByUsername(username: String) = username.let { userSearchApiService.findUserByUsername(username) }

  fun findUsers(
    name: String?,
    roles: List<String>?,
    groups: List<String>?,
    pageable: Pageable,
    status: Status,
  ) = userSearchApiService.findUsers(name, roles, groups, pageable, status)
}

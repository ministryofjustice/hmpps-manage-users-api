package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers.UserSearchApiService

@Service
class UserSearchService(
  val userSearchApiService: UserSearchApiService
) {

  fun findExternalUsersByEmail(email: String?) =
    email ?.let { userSearchApiService.findUsersByEmail(email) }

  fun findExternalUserByUsername(username: String) =
    username.let { userSearchApiService.findUserByUsername(username) }

  fun findUsers(
    name: String?,
    roles: List<String>?,
    groups: List<String>?,
    pageable: Pageable,
    status: Status
  ) = userSearchApiService.findUsers(name, roles, groups, pageable, status)
}

enum class Status {
  ACTIVE, INACTIVE, ALL
}

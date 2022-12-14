package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserDto

@Service
class UserSearchService(
  val externalUsersApiService: ExternalUsersApiService
) {

  fun findExternalUsersByEmail(email: String?): List<UserDto>? =
    email ?.let { externalUsersApiService.findUsersByEmail(email) }

  fun findExternalUsersByUserName(userName: String): UserDto? =
    userName.let { externalUsersApiService.findUsersByUserName(userName) }

  fun findUsers(
    name: String?,
    roles: List<String>?,
    groups: List<String>?,
    pageable: Pageable,
    status: Status) = externalUsersApiService.findUsers(name, roles, groups, pageable, status)
}

enum class Status {
  ACTIVE, INACTIVE, ALL
}

package uk.gov.justice.digital.hmpps.manageusersapi.service.external

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
}

package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserDto

@Service
class UserSearchService(
  val externalUsersApiService: ExternalUsersApiService
) {

  fun findExternalUsersByEmail(email: String?): List<UserDto>? =
    email ?.let { externalUsersApiService.findUsersByEmail(email) }
}

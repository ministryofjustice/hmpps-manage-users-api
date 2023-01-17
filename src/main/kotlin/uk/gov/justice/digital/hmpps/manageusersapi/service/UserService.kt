package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto
import java.util.Optional

@Service
class UserService(
  private val externalUsersSearchApiService: UserSearchApiService
) {
  fun findUserByUsername(username: String): Optional<UserDetailsDto> =
    Optional.ofNullable(externalUsersSearchApiService.findUserByUsernameOrNull(username)?.toUserDetails())
  // or nomis user
  // or delius user
  // or azure user
}

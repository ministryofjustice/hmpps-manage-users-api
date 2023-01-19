package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto

@Service
class UserService(
  private val authApiService: AuthApiService,
  private val externalUsersSearchApiService: UserSearchApiService
) {
  fun findUserByUsername(username: String): UserDetailsDto? =
    externalUsersSearchApiService.findUserByUsernameOrNull(username)?.toUserDetails()
      // or nomis user
      // or delius user
      ?: run {
        authApiService.findAzureUserByUsername(username)
      }
}

package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto

@Service
class UserService(
  private val authApiService: AuthApiService,
  private val deliusApiService: UserApiService,
  private val externalUsersSearchApiService: UserSearchApiService,
) {
  fun findUserByUsername(username: String): UserDetailsDto? =
    externalUsersSearchApiService.findUserByUsernameOrNull(username)?.toUserDetails()
      // or nomis user
      ?: run {
        deliusApiService.findUserByUsername(username)?.toUserDetails()
          ?: run {
            authApiService.findAzureUserByUsername(username)
          }
      }
  // Call to auth to save details (if it doesn't already exist) and get auth uuid to save in json returned
}

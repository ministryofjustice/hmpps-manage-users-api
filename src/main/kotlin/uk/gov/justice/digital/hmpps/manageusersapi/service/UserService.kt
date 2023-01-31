package uk.gov.justice.digital.hmpps.manageusersapi.service

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto

@Service
class UserService(
  private val authApiService: AuthApiService,
  private val deliusApiService: UserApiService,
  private val externalUsersSearchApiService: UserSearchApiService,
  private val authenticationFacade: AuthenticationFacade,
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

  fun myRoles() =
    authenticationFacade.authentication.authorities.filter { (it!!.authority.startsWith("ROLE_")) }
      .map { ExternalUserRole(it!!.authority.substring(5)) }
}

@Schema(description = "User Role")
data class ExternalUserRole(
  @Schema(required = true, description = "Role Code", example = "GLOBAL_SEARCH")
  val roleCode: String,
)

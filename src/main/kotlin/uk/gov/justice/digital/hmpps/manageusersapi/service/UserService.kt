package uk.gov.justice.digital.hmpps.manageusersapi.service

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.model.GenericUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto

@Service
class UserService(
  private val authApiService: AuthApiService,
  private val deliusApiService: UserApiService,
  private val externalUsersSearchApiService: UserSearchApiService,
  private val nomisApiService: uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService,
  private val authenticationFacade: AuthenticationFacade
) {
  fun findUserByUsername(username: String): GenericUser? {
    val userDetails = externalUsersSearchApiService.findUserByUsernameOrNull(username)?.toGenericUser()
      ?: run {
        nomisApiService.findUserByUsername(username)?.toGenericUser()
          ?: run {
            authApiService.findAzureUserByUsername(username)?.toGenericUser()
              ?: run {
                deliusApiService.findUserByUsername(username)?.toGenericUser()
              }
          }
      }
    return userDetails?.apply {
      val authUserDetails = authApiService.findUserByUsernameAndSource(username, this.authSource)
      this.uuid = authUserDetails.uuid
    }
  }

  fun myDetails() = findUserByUsername(authenticationFacade.currentUsername!!)
    ?: UsernameDto(authenticationFacade.currentUsername!!)

  fun myRoles() =
    authenticationFacade.authentication.authorities.filter { (it!!.authority.startsWith("ROLE_")) }
      .map { ExternalUserRole(it!!.authority.substring(5)) }
}

interface User {
  val username: String
}

data class UsernameDto(
  override val username: String
) : User

@Schema(description = "User Role")
data class ExternalUserRole(
  @Schema(required = true, description = "Role Code", example = "GLOBAL_SEARCH")
  val roleCode: String,
)

package uk.gov.justice.digital.hmpps.manageusersapi.service

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserRolesApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.model.GenericUser
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserRole

@Service
class UserService(
  private val authApiService: AuthApiService,
  private val deliusApiService: UserApiService,
  private val externalUsersSearchApiService: UserSearchApiService,
  private val nomisApiService: uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService,
  private val authenticationFacade: AuthenticationFacade,
  private val externalRolesApiService: UserRolesApiService,
) {
  fun findUserByUsername(username: String): GenericUser? {
    val userDetails = externalUsersSearchApiService.findUserByUsernameOrNull(username)
      ?: run {
        nomisApiService.findUserByUsername(username)
          ?: run {
            authApiService.findAzureUserByUsername(username)
              ?: run {
                deliusApiService.findUserByUsername(username)
              }
          }
      }

    return userDetails?.toGenericUser()?.apply {
      val authUserDetails = authApiService.findUserByUsernameAndSource(username, this.authSource)
      this.uuid = authUserDetails.uuid
    }
  }

  fun findRolesByUsername(username: String): List<UserRole>? {
    return externalRolesApiService.findRolesByUsernameOrNull(username)?.map { UserRole(it.roleCode) }
      ?: run { nomisApiService.findUserByUsername(username)?.roles?.map { UserRole(it) } }
      ?: run { authApiService.findAzureUserByUsername(username)?.roles?.map { UserRole(it.name) } }
      ?: run { deliusApiService.findUserByUsername(username)?.roles?.map { UserRole(it.name) } }
  }

  fun myRoles() =
    authenticationFacade.authentication.authorities.filter { (it!!.authority.startsWith("ROLE_")) }
      .map { ExternalUserRole(it!!.authority.substring(5)) }
}

@Schema(description = "User Role")
data class ExternalUserRole(
  @Schema(required = true, description = "Role Code", example = "GLOBAL_SEARCH")
  val roleCode: String,
)

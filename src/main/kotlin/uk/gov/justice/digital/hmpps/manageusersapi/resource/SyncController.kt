package uk.gov.justice.digital.hmpps.manageusersapi.resource
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleSyncService
import uk.gov.justice.digital.hmpps.manageusersapi.service.SyncOptions
import uk.gov.justice.digital.hmpps.manageusersapi.service.SyncStatistics
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserSyncService

@RestController
@RequestMapping(name = "Sync statistics between Nomis and Auth", path = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class SyncController(
  private val roleSyncService: RoleSyncService,
  private val userSyncService: UserSyncService
) {
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN','ROLE_MAINTAIN_ACCESS_ROLES') and hasAuthority('SCOPE_write')")
  @Operation(
    summary = "Determine and update role details between Nomis and Auth, where Auth is King",
    description = "Gets role sync statistics and syncs role data, role required is ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES",
    security = [
      SecurityRequirement(
        name = "ROLE_MAINTAIN_ACCESS_ROLES_ADMIN, ROLE_MAINTAIN_ACCESS_ROLES",
        scopes = ["write"]
      )
    ],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Role Information Synchronised",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = SyncStatistics::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make role sync",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  @PutMapping("/roles")
  fun syncRoles(): SyncStatistics = roleSyncService.sync(false)

  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN','ROLE_MAINTAIN_ACCESS_ROLES')")
  @Operation(
    summary = "Return role differences between Nomis and Auth",
    description = "Gets role sync statistics, role required is ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES",
    security = [
      SecurityRequirement(
        name = "ROLE_MAINTAIN_ACCESS_ROLES_ADMIN, ROLE_MAINTAIN_ACCESS_ROLES",
        scopes = ["read"]
      )
    ],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Role Information Synchronised",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = SyncStatistics::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make role sync",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  @GetMapping("/roles")
  fun syncRolesData(): SyncStatistics = roleSyncService.sync(true)

  @PreAuthorize("hasRole('ROLE_MANAGE_NOMIS_USER_ACCOUNT') and hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Return user email differences between Nomis and Auth",
    description = "Gets user sync statistics, role required is ROLE_MANAGE_NOMIS_USER_ACCOUNT and ROLE_MAINTAIN_OAUTH_USERS",
    security = [
      SecurityRequirement(
        name = "ROLE_MANAGE_NOMIS_USER_ACCOUNT, ROLE_MAINTAIN_OAUTH_USERS",
        scopes = ["read"]
      )
    ],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "User Email Information Synchronised",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = SyncStatistics::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make user sync",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  ) @GetMapping("/users")
  suspend fun syncUsers(
    @RequestParam(value = "caseSensitive", required = false) caseSensitive: Boolean?,
    @RequestParam(value = "usePrimaryEmail", required = false) usePrimaryEmail: Boolean?,
    @RequestParam(value = "onlyVerified", required = false) onlyVerified: Boolean?,
    @RequestParam(value = "domainFilters", required = false) domainFilters: Set<String>?,
  ): SyncStatistics = userSyncService.sync(
    SyncOptions(
      caseSensitive = caseSensitive ?: true,
      usePrimaryEmail = usePrimaryEmail ?: false,
      onlyVerified = onlyVerified ?: false,
      domainFilters = domainFilters ?: emptySet()
    )
  )
}

package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.ExternalUserRolesService
import java.util.UUID
import javax.validation.constraints.NotEmpty

@RestController
@Validated
@RequestMapping("/externalusers", produces = [MediaType.APPLICATION_JSON_VALUE])
class ExternalUserRolesController(
  private val externalUserRolesService: ExternalUserRolesService
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @GetMapping("{userId}/roles")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get list of roles associated with the users account",
    description = "Roles for a specific user. Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES",
    security = [SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES"), SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES_ADMIN")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "User role list"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get roles for a user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get roles for this user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  fun getUserRoles(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
  ) = externalUserRolesService.getUserRoles(userId)

  @DeleteMapping("{userId}/roles/{role}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Remove role from user.",
    description = "Remove role from user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Deleted"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  fun removeRoleByUserId(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
    @Parameter(description = "The role code of the role to be deleted from the user.", required = true)
    @PathVariable
    role: String
  ) {
    externalUserRolesService.removeRoleByUserId(userId, role)
    log.info("Remove role succeeded for userId {} and role code {}", userId, role)
  }

  @PostMapping("/{userId}/roles")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Add roles to user.",
    description = "Add role to user, post version taking multiple roles"
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Added"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "409",
        description = "Role(s) for user already exists..",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  fun addRolesByUserId(
    @Parameter(description = "The user Id of the user.", required = true)
    @PathVariable
    userId: UUID,
    @Parameter(
      description = "List of roles to be assigned.",
      required = true
    )
    @RequestBody
    @NotEmpty
    roles: List<String>
  ) = externalUserRolesService.addRolesByUserId(userId, roles)

  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @GetMapping("{userId}/assignable-roles")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get list of roles associated with the users account",
    description = "Roles for a specific user. Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES",
    security = [SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES"), SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES_ADMIN")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "User role list"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get roles for a user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get roles for this user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  fun getAssignableRoles(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
  ) = externalUserRolesService.getAssignableRoles(userId)
}

@Schema(description = "User Role Details")
data class UserRole(
  @Schema(description = "Role Code", example = "AUTH_GROUP_MANAGER")
  val roleCode: String,

  @Schema(description = "Role Name", example = "Auth Group Manager")
  val roleName: String,

  @Schema(description = "Role Description", example = "Allow Group Manager to administer the account within their groups")
  val roleDescription: String? = null,
)

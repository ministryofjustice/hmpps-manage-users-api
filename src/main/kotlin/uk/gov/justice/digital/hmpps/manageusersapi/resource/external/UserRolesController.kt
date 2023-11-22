package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.constraints.NotEmpty
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.CreateApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.DeleteApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.FailApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.UserRolesService
import java.util.UUID

@RestController("ExternalUserRolesController")
@Validated
@RequestMapping("/externalusers", produces = [MediaType.APPLICATION_JSON_VALUE])
class UserRolesController(
  private val userRolesService: UserRolesService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER', 'ROLE_MAINTAIN_IMS_USERS')")
  @GetMapping("{userId}/roles")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get list of roles associated with the users account",
    description = "Roles for a specific user.<br/>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS, ROLE_AUTH_GROUP_MANAGER or ROLE_MAINTAIN_IMS_USERS",
    security = [
      SecurityRequirement(name = "MAINTAIN_OAUTH_USERS"),
      SecurityRequirement(name = "AUTH_GROUP_MANAGER"),
      SecurityRequirement(name = "ROLE_MAINTAIN_IMS_USERS"),
    ],
  )
  @StandardApiResponses
  fun getUserRoles(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
  ) = userRolesService.getUserRoles(userId)

  @DeleteMapping("{userId}/roles/{role}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER', 'ROLE_MAINTAIN_IMS_USERS')")
  @Operation(
    summary = "Remove role from user.",
    description = "Remove role from user.<br/>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS , ROLE_AUTH_GROUP_MANAGER or ROLE_MAINTAIN_IMS_USERS",
    security = [
      SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS"),
      SecurityRequirement(name = "ROLE_AUTH_GROUP_MANAGER"),
      SecurityRequirement(name = "ROLE_MAINTAIN_IMS_USERS"),
    ],
  )
  @DeleteApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun removeRoleByUserId(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
    @Parameter(description = "The role code of the role to be deleted from the user.", required = true)
    @PathVariable
    role: String,
  ) {
    userRolesService.removeRoleByUserId(userId, role)
    log.info("Remove role succeeded for userId {} and role code {}", userId, role)
  }

  @PostMapping("/{userId}/roles")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER', 'ROLE_MAINTAIN_IMS_USERS')")
  @Operation(
    summary = "Add roles to user.",
    description = "Add role to user, post version taking multiple roles<br/>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS, ROLE_AUTH_GROUP_MANAGER or ROLE_MAINTAIN_IMS_USERS",
    security = [
      SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS"),
      SecurityRequirement(name = "ROLE_AUTH_GROUP_MANAGER"),
      SecurityRequirement(name = "ROLE_MAINTAIN_IMS_USERS"),
    ],
  )
  @CreateApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Role(s) for user already exists..",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun addRolesByUserId(
    @Parameter(description = "The user Id of the user.", required = true)
    @PathVariable
    userId: UUID,
    @Parameter(
      description = "List of roles to be assigned.",
      required = true,
    )
    @RequestBody
    @NotEmpty
    roles: List<String>,
  ) = userRolesService.addRolesByUserId(userId, roles)

  @PutMapping("/{userId}/roles/{role}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER', 'ROLE_MAINTAIN_IMS_USERS')")
  @Operation(
    summary = "Add role to user.",
    description = "Add role to user. Requires role ROLE_MAINTAIN_OAUTH_USERS, ROLE_AUTH_GROUP_MANAGER or ROLE_MAINTAIN_IMS_USERS",
    security = [
      SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS"),
      SecurityRequirement(name = "ROLE_AUTH_GROUP_MANAGER"),
      SecurityRequirement(name = "ROLE_MAINTAIN_IMS_USERS"),
    ],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Added.",
      ),
      ApiResponse(
        responseCode = "403",
        description = "Unable to maintain user, the user is not within one of your groups.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "409",
        description = "Role for user already exists.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun addRoleByUserId(
    @Parameter(description = "The userId of the user.", required = true) @PathVariable
    userId: UUID,
    @Parameter(description = "The role to be added to the user.", required = true) @PathVariable
    role: String,
  ) = userRolesService.addRolesByUserId(userId, listOf(role))

  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER', 'ROLE_MAINTAIN_IMS_USERS')")
  @GetMapping("/{userId}/assignable-roles")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get list of roles associated with the users account",
    description = "Roles for a specific user. Requires role ROLE_MAINTAIN_OAUTH_USERS, ROLE_AUTH_GROUP_MANAGER, ROLE_MAINTAIN_IMS_USERS",
    security = [
      SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS"),
      SecurityRequirement(name = "ROLE_AUTH_GROUP_MANAGER"),
      SecurityRequirement(name = "ROLE_MAINTAIN_IMS_USERS"),
    ],
  )
  @StandardApiResponses
  fun getAssignableRoles(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
  ) = userRolesService.getAssignableRoles(userId)

  @GetMapping("/me/searchable-roles")
  @Operation(
    summary = "Get list of searchable roles.",
    description = "Get list of roles that can be search for by the current user.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun searchableRoles() =
    userRolesService.getAllSearchableRoles()
}

@Schema(description = "User Role Details")
data class UserRole(
  @Schema(description = "Role Code", example = "AUTH_GROUP_MANAGER")
  val roleCode: String,

  @Schema(description = "Role Name", example = "Auth Group Manager")
  val roleName: String,

  @Schema(
    description = "Role Description",
    example = "Allow Group Manager to administer the account within their groups",
  )
  val roleDescription: String? = null,
)

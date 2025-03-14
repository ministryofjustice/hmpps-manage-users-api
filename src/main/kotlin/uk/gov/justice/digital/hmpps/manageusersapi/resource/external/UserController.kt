package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.AuthenticatedApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.FailApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.UserGroupService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.UserService
import java.util.UUID

@RestController("ExternalUserController")
class UserController(
  private val userService: UserService,
  private val userGroupService: UserGroupService,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) {

  @GetMapping("/externalusers/me/assignable-groups")
  @Operation(
    summary = "Get list of assignable groups.",
    description = "Get list of groups that can be assigned by the current user.",
  )
  @AuthenticatedApiResponses
  fun assignableGroups() = userGroupService.getMyAssignableGroups()

  @PutMapping("/externalusers/{userId}/enable")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Enable a user.",
    description = "Enable a user. Requires role ROLE_MAINTAIN_OAUTH_USERS or ROLE_AUTH_GROUP_MANAGER",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS"), SecurityRequirement(name = "ROLE_AUTH_GROUP_MANAGER")],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "OK.",
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
    ],
  )
  fun enableUserByUserId(
    @Parameter(description = "The userId of the user.", required = true) @PathVariable
    userId: UUID,
  ) = userService.enableUserByUserId(
    userId,
  )

  @PutMapping("/externalusers/{userId}/disable")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Disable a user.",
    description = "Disable a user. Requires role ROLE_MAINTAIN_OAUTH_USERS or ROLE_AUTH_GROUP_MANAGER",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS"), SecurityRequirement(name = "ROLE_AUTH_GROUP_MANAGER")],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "OK.",
      ),
      ApiResponse(
        responseCode = "403",
        description = "Unable to enable user, the user is not within one of your groups.",
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
    ],
  )
  fun disableUserByUserId(
    @Parameter(description = "The userId of the user.", required = true)
    @PathVariable
    userId: UUID,
    @Parameter(
      description = "The reason user made inactive.",
      required = true,
    ) @RequestBody
    deactivateReason: DeactivateReason,
  ) = userService.disableUserByUserId(
    userId,
    deactivateReason,
  )

  @PostMapping("/externalusers/{userId}/email")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Amend a user email address.",
    description = "Amend a user email address. Requires role ROLE_MAINTAIN_OAUTH_USERS or ROLE_AUTH_GROUP_MANAGER",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS"), SecurityRequirement(name = "ROLE_AUTH_GROUP_MANAGER")],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "OK.",
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
    ],
  )
  fun alterUserEmail(
    @Parameter(description = "The ID of the user.", required = true) @PathVariable
    userId: UUID,
    @RequestBody amendUser: AmendUser,
    @Parameter(hidden = true) request: HttpServletRequest,
  ): String? {
    val resetLink = userService.amendUserEmailByUserId(
      userId,
      amendUser.email,
    )
    return if (smokeTestEnabled) resetLink else null
  }

  @PostMapping("/externalusers/create")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Create user",
    description = "Create user. Requires role ROLE_MAINTAIN_OAUTH_USERS or ROLE_AUTH_GROUP_MANAGER",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS"), SecurityRequirement(name = "ROLE_AUTH_GROUP_MANAGER")],
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "409",
        description = "User or email already exists.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createUserByEmail(
    @Parameter(description = "Details of the user to be created.", required = true) @RequestBody
    user: NewUser,
  ): ResponseEntity<Any> = ResponseEntity.ok(userService.createUser(user))
}

data class NewUser(
  @Schema(
    required = true,
    description = "Email address",
    example = "nomis.user@someagency.justice.gov.uk",
  )
  val email: String,

  @Schema(required = true, description = "First name", example = "Nomis")
  val firstName: String,

  @Schema(required = true, description = "Last name", example = "User")
  val lastName: String,

  @Schema(
    description = "Initial groups, can be used for one or more initial groups",
    example = "[\"SITE_1_GROUP_1\", \"SITE_1_GROUP_2\"]",
  )
  val groupCodes: Set<String>?,
)

data class AmendUser(
  @Schema(required = true, description = "Email address", example = "nomis.user@someagency.justice.gov.uk")
  val email: String?,
)

@Schema(description = "Deactivate Reason")
data class DeactivateReason(
  @Schema(required = true, description = "Deactivate Reason", example = "User has left")
  @field:Size(max = 100, min = 4, message = "Reason must be between 4 and 100 characters")
  @NotBlank
  val reason: String,
)

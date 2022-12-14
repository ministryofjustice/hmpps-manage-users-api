package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.UserGroupService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.UserService
import java.util.UUID
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController("ExternalUserController")
class UserController(
  private val userService: UserService,
  private val userGroupService: UserGroupService
) {

  @GetMapping("/externalusers/me/assignable-groups")
  @Operation(
    summary = "Get list of assignable groups.",
    description = "Get list of groups that can be assigned by the current user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK"
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
      )
    ]
  )
  fun assignableGroups() = userGroupService.getMyAssignableGroups()

  @PutMapping("/users/{userId}/enable")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Enable a user.",
    description = "Enable a user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "OK."
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
        responseCode = "403",
        description = "Unable to enable user, the user is not within one of your groups.",
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
  fun enableUserByUserId(
    @Parameter(description = "The userId of the user.", required = true) @PathVariable
    userId: UUID
  ) = userService.enableUserByUserId(
    userId
  )

  @PutMapping("/users/{userId}/disable")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Disable a user.",
    description = "Disable a user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "OK."
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
        responseCode = "403",
        description = "Unable to enable user, the user is not within one of your groups.",
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
  fun disableUserByUserId(
    @Parameter(description = "The userId of the user.", required = true) @PathVariable
    userId: UUID,
    @Parameter(
      description = "The reason user made inactive.",
      required = true
    ) @org.springframework.web.bind.annotation.RequestBody
    deactivateReason: DeactivateReason
  ) = userService.disableUserByUserId(
    userId,
    deactivateReason
  )
}

@Schema(description = "Deactivate Reason")
data class DeactivateReason(
  @Schema(required = true, description = "Deactivate Reason", example = "User has left")
  @field:Size(max = 100, min = 4, message = "Reason must be between 4 and 100 characters") @NotBlank
  val reason: String
)

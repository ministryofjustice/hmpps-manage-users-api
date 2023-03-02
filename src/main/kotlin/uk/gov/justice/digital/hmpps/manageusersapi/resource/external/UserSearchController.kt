package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.Status
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.UserSearchService
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/externalusers")
class UserSearchController(
  private val userSearchService: UserSearchService
) {

  @GetMapping("/search")
  @Operation(
    summary = "Search for an external user."
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
  @PreAuthorize(
    "hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')"
  )
  suspend fun searchForUser(
    @Parameter(
      description = "The username, email or name of the user.",
      example = "j smith"
    ) @RequestParam(required = false)
    name: String?,
    @Parameter(description = "The role codes of the user.") @RequestParam(required = false)
    roles: List<String>?,
    @Parameter(description = "The group codes of the user.") @RequestParam(required = false)
    groups: List<String>?,
    @Parameter(description = "Limit to active / inactive / show all users.") @RequestParam(
      required = false,
      defaultValue = "ALL"
    )
    status: Status,
    @RequestParam(value = "page", defaultValue = "0", required = false) page: Int,
    @RequestParam(value = "size", defaultValue = "10", required = false) size: Int
  ) =
    userSearchService.findUsers(
      name,
      roles,
      groups,
      PageRequest.of(page, size),
      status
    )

  @GetMapping
  @Operation(
    summary = "Search for a user.",
    description = "Search for a user."
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ExternalUserDetailsDto::class)
          )
        ]
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
        responseCode = "204",
        description = "No users found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  suspend fun searchForUser(
    @Parameter(description = "The email address of the user.", required = true) @RequestParam
    email: String?
  ): ResponseEntity<Any> {
    val users = userSearchService.findExternalUsersByEmail(email)
    return if (users == null) ResponseEntity.noContent().build() else ResponseEntity.ok(users)
  }

  @GetMapping("/id/{userId}")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Retrieve user details by user id.",
    description = "Retrieve detail by user id. Note that when accessing with Group Manager role the accessor must have a group in common with the user"
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
      ),
      ApiResponse(
        responseCode = "403",
        description = "Unable to view user, either you do not have authority or the user is not within one of your groups.",
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
  fun userById(
    @Parameter(description = "The id of the user.", required = true) @PathVariable
    userId: UUID
  ): ExternalUserDetailsDto {
    return ExternalUserDetailsDto.fromDomain(userSearchService.findExternalUserById(userId))
  }

  @GetMapping("/{username}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "User detail.",
    description = "User detail."
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
  fun user(
    @Parameter(description = "The username of the user.", required = true) @PathVariable
    username: String
  ) = userSearchService.findExternalUserByUsername(username)
}

data class ExternalUserDetailsDto(
  @Schema(description = "User ID", example = "91229A16-B5F4-4784-942E-A484A97AC865")
  val userId: UUID,

  @Schema(description = "Username", example = "externaluser")
  val username: String,

  @Schema(description = "Email address", example = "external.user@someagency.justice.gov.uk")
  val email: String,

  @Schema(description = "First name", example = "External")
  val firstName: String,

  @Schema(description = "Last name", example = "User")
  val lastName: String,

  @Schema(description = "Account is locked due to incorrect password attempts", example = "true")
  val locked: Boolean = false,

  @Schema(required = true, description = "Account is enabled", example = "false")
  val enabled: Boolean = false,

  @Schema(required = true, description = "Email address has been verified", example = "false")
  val verified: Boolean = false,

  @Schema(required = true, description = "Last time user logged in", example = "01/01/2001")
  val lastLoggedIn: LocalDateTime? = null,

  @Schema(required = true, description = "Inactive reason", example = "Left department")
  val inactiveReason: String? = null
) {
  companion object {
    fun fromDomain(user: ExternalUser): ExternalUserDetailsDto {
      with(user) {
        return ExternalUserDetailsDto(userId, username, email, firstName, lastName, locked, enabled, verified, lastLoggedIn, inactiveReason)
      }
    }
  }
}

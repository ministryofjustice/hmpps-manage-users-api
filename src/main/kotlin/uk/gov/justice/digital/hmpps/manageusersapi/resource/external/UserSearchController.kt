package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.UserSearchService
import java.time.LocalDateTime

@RestController
@RequestMapping("/externalusers")
class ExternalUserSearchController(
  private val userSearchService: UserSearchService
) {

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
            schema = Schema(implementation = UserDto::class)
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
  ) = userSearchService.findExternalUsersByUserName(username)
}

data class UserDto(
  @Schema(
    required = true,
    description = "User ID",
    example = "91229A16-B5F4-4784-942E-A484A97AC865"
  )
  val userId: String? = null,

  @Schema(required = true, description = "Username", example = "externaluser")
  val username: String? = null,

  @Schema(
    required = true,
    description = "Email address",
    example = "external.user@someagency.justice.gov.uk"
  )
  val email: String? = null,

  @Schema(required = true, description = "First name", example = "External")
  val firstName: String? = null,

  @Schema(required = true, description = "Last name", example = "User")
  val lastName: String? = null,

  @Schema(
    required = true,
    description = "Account is locked due to incorrect password attempts",
    example = "true"
  )
  val locked: Boolean = false,

  @Schema(required = true, description = "Account is enabled", example = "false")
  val enabled: Boolean = false,

  @Schema(required = true, description = "Email address has been verified", example = "false")
  val verified: Boolean = false,

  @Schema(required = true, description = "Last time user logged in", example = "01/01/2001")
  val lastLoggedIn: LocalDateTime? = null,

  @Schema(required = true, description = "Inactive reason", example = "Left department")
  val inactiveReason: String? = null
)

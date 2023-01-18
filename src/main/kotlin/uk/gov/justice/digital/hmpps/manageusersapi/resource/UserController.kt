package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.service.NotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserService

@RestController("UserController")
class UserController(
  private val userService: UserService,
) {

  @GetMapping("/users/{username}")
  @Operation(
    summary = "User detail.",
    description = "Find user detail by username."
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
  fun findUser(
    @Parameter(description = "The username of the user.", required = true)
    @PathVariable
    username: String
  ): UserDetailsDto = userService.findUserByUsername(username)
    .orElseThrow { NotFoundException("Account for username $username not found") }
}

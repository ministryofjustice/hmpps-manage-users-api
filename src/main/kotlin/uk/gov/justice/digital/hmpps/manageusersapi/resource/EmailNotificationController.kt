package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailNotificationService
import javax.validation.Valid

@RestController
@RequestMapping("/notify")
class EmailNotificationController(
  private val emailNotificationService: EmailNotificationService
) {

  @Operation(
    summary = "Sends Email for enabling user",
    description = "Sends Email for enabling user, role required is ROLE_MAINTAIN_OAUTH_USERS or ROLE_AUTH_GROUP_MANAGER",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Email sent for enabled user",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_ROLES_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
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
  @PostMapping("enable-user")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  fun sendEnableEmail(
    @Schema(description = "Details of the email domain to be created.", required = true)
    @RequestBody @Valid emailNotificationDto: EmailNotificationDto
  ) {
    emailNotificationService.sendEnableEmail(emailNotificationDto)
  }
}

data class EmailNotificationDto(
  @Schema(description = "Username", example = "TEST_USER")
  val username: String,

  @Schema(description = "First name of the user", example = "John")
  val firstName: String,

  @Schema(description = "email of the user", example = "Smith@gov.uk")
  val email: String,

  @Schema(description = "admin id who enabled user", example = "ADMIN_USR")
  val admin: String

)

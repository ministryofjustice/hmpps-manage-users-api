package uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.NomisUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.UserService
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

@RestController("NomisUserController")
@Validated
class UserController(
  private val nomisUserService: UserService
) {
  @PostMapping("/prisonusers", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasRole('ROLE_CREATE_USER')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a DPS user",
    description = "Creates a specific DPS user. Requires role ROLE_CREATE_USER",
    security = [SecurityRequirement(name = "ROLE_CREATE_USER")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Create a DPS user",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(
              implementation = NomisUserDetails::class
            )
          )
        ]
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to create DPS user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create this user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      )
    ]
  )
  fun createUser(
    @RequestBody @Valid
    createUserRequest: CreateUserRequest
  ) = nomisUserService.createUser(createUserRequest)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DPS User creation")
data class CreateUserRequest(
  @Schema(description = "Username", example = "TEST_USER", required = true)
  @NotBlank
  val username: String,

  @Schema(description = "Email Address", example = "test@justice.gov.uk", required = true)
  @field:Email(message = "Not a valid email address")
  @NotBlank
  val email: String,

  @Schema(description = "First name of the user", example = "John", required = true)
  @NotBlank
  val firstName: String,

  @Schema(description = "Last name of the user", example = "Smith", required = true)
  @NotBlank
  val lastName: String,

  @Schema(description = "The type of user", example = "DPS_LSA", required = true)
  @NotBlank
  val userType: UserType,

  @Schema(description = "Default caseload (a.k.a Prison ID)", example = "BXI", required = false)
  val defaultCaseloadId: String? = null
)

enum class UserType {
  DPS_ADM,
  DPS_GEN,
  DPS_LSA,
}

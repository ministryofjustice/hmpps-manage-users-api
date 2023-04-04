package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.apache.commons.text.WordUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.NewPrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.UsageType
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserService
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@RestController("NomisUserController")
@Validated
class UserController(
  private val nomisUserService: UserService,
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
              implementation = NewPrisonUserDto::class,
            ),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to create DPS user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create this user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createUser(
    @RequestBody @Valid
    createUserRequest: CreateUserRequest,
  ) = nomisUserService.createUser(createUserRequest)

  @PostMapping("/linkedprisonusers/admin", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasRole('ROLE_CREATE_USER')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a Linked DPS Admin user",
    description = "Creates a specific Linked DPS user. Requires role ROLE_CREATE_USER",
    security = [SecurityRequirement(name = "ROLE_CREATE_USER")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Create a Linked DPS Admin user",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(
              implementation = PrisonStaffUserDto::class,
            ),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to create linked DPS Admin user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to create this user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createLinkedUser(
    @RequestBody @Valid
    createLinkedAdminUserRequest: CreateLinkedAdminUserRequest,
  ) = nomisUserService.createLinkedUser(createLinkedAdminUserRequest)

  @GetMapping("/prisonusers", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_USE_OF_FORCE', 'ROLE_STAFF_SEARCH')")
  @Operation(
    summary = "Find prison users by first and last name.",
    description = "Find prison users by first and last name.",
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
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_USE_OF_FORCE or ROLE_STAFF_SEARCH",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun findUsersByFirstAndLastName(
    @Parameter(
      description = "The first name to match. Case insensitive.",
      required = true,
    ) @RequestParam @NotEmpty
    firstName: String,
    @Parameter(
      description = "The last name to match. Case insensitive",
      required = true,
    ) @RequestParam @NotEmpty
    lastName: String,
  ): List<PrisonUserDto> = nomisUserService.findUsersByFirstAndLastName(firstName, lastName)
    .map {
      PrisonUserDto(
        username = it.username,
        staffId = it.userId.toLongOrNull(),
        email = it.email,
        verified = it.verified,
        firstName = WordUtils.capitalizeFully(it.firstName),
        lastName = WordUtils.capitalizeFully(it.lastName),
        name = WordUtils.capitalizeFully("${it.firstName} ${it.lastName}"),
        activeCaseLoadId = it.activeCaseLoadId,
      )
    }
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
  val defaultCaseloadId: String? = null,

  @Schema(description = "Indicates if the new user account should be linked", example = "true", required = false)
  val linkAccount: Boolean? = false,
)

enum class UserType {
  DPS_ADM,
  DPS_GEN,
  DPS_LSA,
}

data class PrisonUserDto(
  @Schema(required = true, example = "RO_USER_TEST")
  val username: String,
  @Schema(required = true, example = "1234564789")
  val staffId: Long?,
  @Schema(required = false, example = "ryanorton@justice.gov.uk")
  val email: String?,
  @Schema(required = true, example = "true")
  val verified: Boolean,
  @Schema(required = true, example = "Ryan")
  val firstName: String,
  @Schema(required = true, example = "Orton")
  val lastName: String,
  @Schema(required = true, example = "Ryan Orton")
  val name: String,
  @Schema(required = false, example = "MDI")
  val activeCaseLoadId: String?,
)

@Schema(description = "Nomis User Created Details")
data class NewPrisonUserDto(
  @Schema(description = "Username", example = "TEST_USER")
  val username: String,

  @Schema(description = "Email Address", example = "test@justice.gov.uk")
  val primaryEmail: String,

  @Schema(description = "First name of the user", example = "John")
  val firstName: String,

  @Schema(description = "Last name of the user", example = "Smith")
  val lastName: String,
) {
  companion object {
    fun fromDomain(newPrisonUser: NewPrisonUser): NewPrisonUserDto {
      with(newPrisonUser) {
        return NewPrisonUserDto(username, primaryEmail, firstName, lastName)
      }
    }
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Linking a new admin account to an existing general user")
data class CreateLinkedAdminUserRequest(
  @Schema(description = "existingUsername", example = "TESTUSER1", required = true)
  @field:Size(
    max = 30,
    min = 1,
    message = "Username must be between 1 and 30",
  )
  @NotBlank
  val existingUsername: String,

  @Schema(description = "adminUsername", example = "TESTUSER1_ADM", required = true)
  @field:Size(
    max = 30,
    min = 1,
    message = "Username must be between 1 and 30",
  )
  @NotBlank
  val adminUsername: String,
)

data class PrisonStaffUserDto(
  val staffId: Long,
  val firstName: String,
  val lastName: String,
  val status: String,
  val primaryEmail: String?,
  val generalAccount: UserCaseloadDetailDto?,
  val adminAccount: UserCaseloadDetailDto?,
)

data class UserCaseloadDetailDto(
  val username: String,
  val active: Boolean,
  val accountType: UsageType = UsageType.GENERAL,
  val activeCaseload: PrisonCaseload?,
  val caseloads: List<PrisonCaseload>? = listOf(),
)

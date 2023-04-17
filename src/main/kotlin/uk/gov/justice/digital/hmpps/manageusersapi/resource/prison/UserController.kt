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
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonStaffUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.FailApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserService
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Size

@RestController("PrisonUserController")
@Validated
class UserController(
  private val prisonUserService: UserService,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) {
  @PostMapping("/prisonusers/{username}/email")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN')")
  @Operation(
    summary = "Amend a prison user email address.",
    description = "Amend a prison user email address.",
  )
  @StandardApiResponses
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
  fun amendUserEmail(
    @Parameter(description = "The username of the user.", required = true) @PathVariable
    username: String,
    @Valid @RequestBody
    amendEmail: AmendEmail,
  ): String? {
    val link = prisonUserService.changeEmail(username, amendEmail.email!!)
    return if (smokeTestEnabled) link else ""
  }

  @PostMapping("/prisonusers", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasRole('ROLE_CREATE_USER')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create a DPS user",
    description = "Creates a specific DPS user. Requires role ROLE_CREATE_USER",
    security = [SecurityRequirement(name = "ROLE_CREATE_USER")],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
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
    ],
  )
  fun createUser(
    @RequestBody @Valid
    createUserRequest: CreateUserRequest,
  ) = NewPrisonUserDto.fromDomain(prisonUserService.createUser(createUserRequest))

  @PostMapping("/linkedprisonusers/admin", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasRole('ROLE_CREATE_USER')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Link an Admin User to an existing General Account",
    description = "Link an Admin User to an existing General Account. Requires role ROLE_CREATE_USER",
    security = [SecurityRequirement(name = "ROLE_CREATE_USER")],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Admin User linked to an existing General Account",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(
              implementation = PrisonStaffUserDto::class,
            ),
          ),
        ],
      ),
    ],
  )
  fun createLinkedCentralAdminUser(
    @RequestBody @Valid
    createLinkedCentralAdminUserRequest: CreateLinkedCentralAdminUserRequest,
  ) = PrisonStaffUserDto.fromDomain(prisonUserService.createLinkedCentralAdminUser(createLinkedCentralAdminUserRequest))

  @PostMapping("/linkedprisonusers/lsa", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasRole('ROLE_CREATE_USER')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Link a Local Admin User to an existing General Account",
    description = "Link a Local Admin User to an existing General Account. Requires role ROLE_CREATE_USER",
    security = [SecurityRequirement(name = "ROLE_CREATE_USER")],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Local Admin User linked to an existing General Account",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(
              implementation = PrisonStaffUserDto::class,
            ),
          ),
        ],
      ),
    ],
  )
  fun createLinkedLocalAdminUser(
    @RequestBody @Valid
    createLinkedLocalAdminUserRequest: CreateLinkedLocalAdminUserRequest,
  ) = PrisonStaffUserDto.fromDomain(prisonUserService.createLinkedLocalAdminUser(createLinkedLocalAdminUserRequest))

  @GetMapping("/prisonusers", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_USE_OF_FORCE', 'ROLE_STAFF_SEARCH')")
  @Operation(
    summary = "Find prison users by first and last name.",
    description = "Find prison users by first and last name.",
  )
  @StandardApiResponses
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
  ): List<PrisonUserDto> = prisonUserService.findUsersByFirstAndLastName(firstName, lastName)
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

@Schema(description = "Prison User Created Details")
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
    fun fromDomain(newPrisonUser: PrisonUser): NewPrisonUserDto {
      with(newPrisonUser) {
        return NewPrisonUserDto(username, email!!, firstName, lastName)
      }
    }
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Linking a new Central admin account to an existing general user")
data class CreateLinkedCentralAdminUserRequest(
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

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Linking a new Local admin account to an existing general user")
data class CreateLinkedLocalAdminUserRequest(
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

  @Schema(description = "Default local admin group (prison) to manage users", example = "MDI", required = true)
  @field:Size(
    max = 6,
    min = 3,
    message = "Admin group must be between 3-6 characters",
  )
  @NotBlank
  val localAdminGroup: String,
)

data class AmendEmail(
  @Schema(required = true, description = "Email address", example = "prison.user@someagency.justice.gov.uk")
  @field:NotBlank(message = "Email must not be blank")
  val email: String?,
)

data class PrisonStaffUserDto(
  val staffId: Long,
  val firstName: String,
  val lastName: String,
  val status: String,
  val primaryEmail: String?,
  val generalAccount: UserCaseload?,
  val adminAccount: UserCaseload?,
) {
  companion object {
    fun fromDomain(prisonStaffUser: PrisonStaffUser): PrisonStaffUserDto {
      with(prisonStaffUser) {
        return PrisonStaffUserDto(
          staffId,
          firstName,
          lastName,
          status,
          primaryEmail,
          generalAccount,
          adminAccount,
        )
      }
    }
  }
}

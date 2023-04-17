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
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonStaffUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUsageType
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseload
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
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request e.g. missing email address.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
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
  ) = NewPrisonUserDto.fromDomain(prisonUserService.createUser(createUserRequest))

  @PostMapping("/linkedprisonusers/admin", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasRole('ROLE_CREATE_USER')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Link an Admin User to an existing General Account",
    description = "Link an Admin User to an existing General Account. Requires role ROLE_CREATE_USER",
    security = [SecurityRequirement(name = "ROLE_CREATE_USER")],
    responses = [
      ApiResponse(
        responseCode = "201",
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
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to link an admin user to a general user",
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
        description = "Incorrect permissions to link an admin user to an existing general user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
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
    responses = [
      ApiResponse(
        responseCode = "201",
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
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to link a local admin user to a general user",
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
        description = "Incorrect permissions to link a local admin user to an existing general user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createLinkedLocalAdminUser(
    @RequestBody @Valid
    createLinkedLocalAdminUserRequest: CreateLinkedLocalAdminUserRequest,
  ) = PrisonStaffUserDto.fromDomain(prisonUserService.createLinkedLocalAdminUser(createLinkedLocalAdminUserRequest))

  @PostMapping("/linkedprisonusers/general", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasRole('ROLE_CREATE_USER')")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Link a New General User to an existing Admin Account",
    description = "Link a New General User to an existing Admin Account. Requires role ROLE_CREATE_USER",
    security = [SecurityRequirement(name = "ROLE_CREATE_USER")],
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "General User linked to an existing Admin Account",
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
        description = "Incorrect request to link a general user to an admin user",
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
        description = "Incorrect permissions to link a general user to an existing admin user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createLinkedGeneralUser(
    @RequestBody @Valid
    createLinkedGeneralUserRequest: CreateLinkedGeneralUserRequest,
  ) = PrisonStaffUserDto.fromDomain(prisonUserService.createLinkedGeneralUser(createLinkedGeneralUserRequest))

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

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Linking a new General account to an existing admin user account")
data class CreateLinkedGeneralUserRequest(
  @Schema(description = "existing admin username", example = "TESTUSER1_ADM", required = true)
  @field:Size(
    max = 30,
    min = 1,
    message = "Admin Username must be between 1 and 30",
  )
  @NotBlank
  val existingAdminUsername: String,

  @Schema(description = "new general username", example = "TESTUSER1_GEN", required = true)
  @field:Size(
    max = 30,
    min = 1,
    message = "Username must be between 1 and 30",
  )
  @NotBlank
  val generalUsername: String,

  @Schema(description = "Default caseload (a.k.a Prison ID), not required for admin accounts", example = "BXI", required = true)
  @field:Size(
    max = 6,
    min = 3,
    message = "Caseload must be between 3-6 characters",
  )
  @NotBlank
  val defaultCaseloadId: String,
)

data class AmendEmail(
  @Schema(required = true, description = "Email address", example = "prison.user@someagency.justice.gov.uk")
  @field:NotBlank(message = "Email must not be blank")
  val email: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison Staff Information")
data class PrisonStaffUserDto(
  @Schema(description = "Staff ID", example = "324323", required = true) val staffId: Long,
  @Schema(description = "First name of the user", example = "John", required = true) val firstName: String,
  @Schema(description = "Last name of the user", example = "Smith", required = true) val lastName: String,
  @Schema(description = "Status of staff account", example = "Smith", required = true) val status: String,
  @Schema(description = "Email addresses of staff", example = "test@test.com", required = false) val primaryEmail: String?,
  @Schema(description = "General user account for this staff member", required = false) val generalAccount: UserCaseloadDto?,
  @Schema(description = "Admin user account for this staff member", required = false) val adminAccount: UserCaseloadDto?,
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
          generalAccount?.let { UserCaseloadDto.fromDomain(it) },
          adminAccount?.let { UserCaseloadDto.fromDomain(it) },
        )
      }
    }
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User & Caseload Information")
data class UserCaseloadDto(
  @Schema(description = "User name", example = "John1", required = true) val username: String,
  @Schema(description = "Indicates that the user is active", example = "true", required = true) val active: Boolean,
  @Schema(description = "Type of user account", example = "GENERAL", required = true) val accountType: PrisonUsageType = PrisonUsageType.GENERAL,
  @Schema(description = "Active Caseload of the user", example = "BXI", required = false) val activeCaseload: PrisonCaseload?,
  @Schema(description = "Caseloads available for this user", required = false) val caseloads: List<PrisonCaseload>? = listOf(),
) {
  companion object {
    fun fromDomain(userCaseload: UserCaseload) = UserCaseloadDto(
      userCaseload.username,
      userCaseload.active,
      userCaseload.accountType,
      userCaseload.activeCaseload,
      userCaseload.caseloads,
    )
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorDetail
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailAddress
import uk.gov.justice.digital.hmpps.manageusersapi.model.GenericUser
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserService
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.NotFoundException
import java.util.UUID

@RestController("UserController")
class UserController(
  private val userService: UserService,
  private val authenticationFacade: AuthenticationFacade,
) {

  @GetMapping("/users/{username}")
  @Operation(
    summary = "User detail.",
    description = "Find user detail by username.",
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
  fun findUser(
    @Parameter(description = "The username of the user.", required = true)
    @PathVariable
    username: String,
  ): UserDetailsDto {
    val user = userService.findUserByUsername(username)
    return if (user != null) {
      UserDetailsDto.fromDomain(user)
    } else {
      throw NotFoundException("Account for username $username not found")
    }
  }

  @GetMapping("/users/me")
  @Operation(
    summary = "My User details.",
    description = "Find my user details.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = UserDetailsDto::class),
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
    ],
  )
  fun myDetails(): User {
    val user = userService.findUserByUsername(authenticationFacade.currentUsername!!)
    return user?.let {
      UserDetailsDto.fromDomain(user)
    } ?: UsernameDto(authenticationFacade.currentUsername!!)
  }

  @GetMapping("/users/{username}/email")
  @Operation(
    summary = "Email address for user",
    description = "Verified email address for user",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "204",
        description = "No content.  No verified email address found for user",
      ),
      ApiResponse(
        responseCode = "404",
        description = "User not found. The user doesn't exist so could have never logged in",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorDetail::class),
          ),
        ],
      ),
    ],
  )
  fun getUserEmail(
    @Parameter(description = "The username of the user.", required = true) @PathVariable
    username: String,
    @Parameter(description = "Return unverified email addresses.", required = false)
    @RequestParam
    unverified: Boolean = false,
  ): ResponseEntity<*> = userService.findUserEmail(username, unverified)
    ?.let {
      if (it.verified || unverified) {
        ResponseEntity.ok(EmailAddressDto(it))
      } else {
        ResponseEntity.noContent().build<Any>()
      }
    } ?: throw NotFoundException("Account for username $username not found")

  @GetMapping("/users/me/roles")
  @Operation(
    summary = "List of roles for current user.",
    description = "List of roles for current user.",
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
    ],
  )
  fun myRoles() = userService.myRoles()

  @GetMapping("/users/{username}/roles")
  @Operation(
    summary = "List of roles for user.",
    description = "List of roles for user. Currently restricted to service specific roles: ROLE_INTEL_ADMIN or ROLE_PF_USER_ADMIN or ROLE_PCMS_USER_ADMIN." +
      "***Change to old endpoint in Auth** 1)  Nomis / Prision user doesn't return additional role in the list:  PRISON " +
      "                                         2)  Delius user doesn't return additional role in the list:  PROBATION ",
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
  @PreAuthorize("hasAnyRole('ROLE_INTEL_ADMIN', 'ROLE_PCMS_USER_ADMIN','ROLE_PF_USER_ADMIN')")
  fun userRoles(
    @Parameter(description = "The username of the user.", required = true) @PathVariable
    username: String,
  ): List<UserRole> {
    return userService.findRolesByUsername(username)
      ?: throw NotFoundException("Account for username $username not found")
  }

  @GetMapping("/roles/delius")
  @Operation(
    summary = "List of mapped delius roles",
    description = "List of mapped  delius roles",
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
    ],
  )
  fun findMappedDeliusRoles(
    @RequestParam(value = "deliusRoles", required = true)
    deliusRoles: List<String>,
  ) = userService.getMappedDeliusRoles(deliusRoles)
}

@Schema(description = "User Role")
data class UserRole(
  @Schema(required = true, description = "Role Code", example = "GLOBAL_SEARCH")
  val roleCode: String,
)

interface User {
  val username: String
}

data class UsernameDto(
  override val username: String,
) : User

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User email details")
data class EmailAddressDto(
  @Schema(required = true, description = "Username", example = "DEMO_USER1")
  val username: String,

  @Schema(description = "Email", example = "john.smith@digital.justice.gov.uk")
  val email: String?,

  @Schema(required = true, description = "Verified email", example = "true")
  val verified: Boolean,
) {
  constructor(emailAddress: EmailAddress) : this(emailAddress.username, emailAddress.email, emailAddress.verified)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User Details")
data class UserDetailsDto(
  @Schema(description = "Username", example = "DEMO_USER1")
  override val username: String,

  @Schema(description = "Active", example = "false")
  var active: Boolean,

  @Schema(description = "Name", example = "John Smith")
  var name: String,

  @Schema(title = "Authentication Source", description = "auth for external users, nomis for nomis authenticated users", example = "nomis")
  var authSource: AuthSource,

  @Deprecated("")
  @Schema(title = "Staff Id", description = "Deprecated, use userId instead", example = "231232")
  var staffId: Long? = null,

  @Deprecated("")
  @Schema(title = "Current Active Caseload", description = "Deprecated, retrieve from prison API rather than manage users", example = "MDI")
  var activeCaseLoadId: String? = null,

  @Schema(title = "User Id", description = "Unique identifier for user, will be UUID for external users or staff ID for nomis users", example = "231232")
  var userId: String,

  @Schema(title = "Unique Id", description = "Universally unique identifier for user, generated and stored in auth database for all users", example = "5105a589-75b3-4ca0-9433-b96228c1c8f3")
  var uuid: UUID? = null,

) : User {

  companion object {
    fun fromDomain(user: GenericUser): UserDetailsDto {
      with(user) {
        return UserDetailsDto(
          username,
          active,
          name,
          authSource,
          staffId,
          activeCaseLoadId,
          userId,
          uuid,
        )
      }
    }
  }
}

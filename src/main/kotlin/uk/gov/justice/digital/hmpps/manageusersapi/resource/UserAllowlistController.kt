package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.CreateApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.Status
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.UserAllowlistService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@RestController("UserAllowlistController")
@Validated
@RequestMapping("/users/allowlist", produces = [MediaType.APPLICATION_JSON_VALUE])
class UserAllowlistController(
  private val userAllowlistService: UserAllowlistService,
) {
  @PostMapping
  @PreAuthorize("hasRole('ROLE_MANAGE_USER_ALLOW_LIST')")
  @Operation(
    summary = "Add a user to the allow list.",
    description = "Add a user to the allow list",
  )
  @CreateApiResponses
  @ResponseStatus(HttpStatus.CREATED)
  fun addUser(
    @Valid
    @Parameter(description = "The add user request")
    @RequestBody
    addUserRequest: UserAllowlistAddRequest,
  ) = userAllowlistService.addUser(addUserRequest)

  @GetMapping
  @PreAuthorize("hasRole('ROLE_MANAGE_USER_ALLOW_LIST')")
  @Operation(
    summary = "Get all allow list users.",
    description = "Get all allow list users, optionally filtering on name and status",
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "The allow list user does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAllAllowlistUsers(
    @Parameter(description = "username, email, first name or last name filter")
    @RequestParam(
      required = false,
    )
    name: String?,
    @Parameter(description = "Expired or Active filter")
    @RequestParam(
      required = false,
      defaultValue = "ALL",
    )
    status: Status,
    @PageableDefault(sort = ["allowlistEndDate"], direction = Sort.Direction.ASC)
    pageable: Pageable,
  ): PagedResponse<UserAllowlistDetail> = userAllowlistService.getAllUsers(name, status, pageable)

  @PatchMapping("/{id}")
  @PreAuthorize("hasRole('ROLE_MANAGE_USER_ALLOW_LIST')")
  @Operation(
    summary = "Update a user's access on the allow list.",
    description = "Update a user's access on the allow list",
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "The allow list user does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  fun updateUserAccess(
    @Parameter(description = "The ID of the allow list user.", required = true)
    @PathVariable
    id: UUID,
    @Valid
    @Parameter(description = "The update user request")
    @RequestBody
    updateUserAccessRequest: UserAllowlistPatchRequest,
  ) = userAllowlistService.updateUserAccess(id, updateUserAccessRequest)

  @GetMapping("/{username}")
  @PreAuthorize("hasRole('ROLE_MANAGE_USER_ALLOW_LIST')")
  @Operation(
    summary = "Get a user on the allow list.",
    description = "Get a user on the allow list",
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "The allow list user does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getAllowlistUser(
    @Parameter(description = "The username of the allow list user.", required = true)
    @PathVariable
    username: String,
  ) = userAllowlistService.getUser(username)
}

@Schema(description = "Add user to allow list")
data class UserAllowlistAddRequest(
  @Schema(
    required = true,
    description = "NOMIS, nDelius or auth username (can also be an email)",
    examples = ["TD00012", "eliseo.hassen@justice.gov.uk"],
  )
  val username: String,

  @Schema(
    required = true,
    description = "Email",
    example = "kellianne.granados@justice.gov.uk",
  )
  val email: String,

  @Schema(
    required = true,
    description = "First name",
    example = "Kellianne",
  )
  val firstName: String,

  @Schema(
    required = true,
    description = "Last name",
    example = "Granados",
  )
  val lastName: String,

  @Schema(
    required = true,
    description = "A valid business reason for granting access to the environment",
    example = "Access is required to allow updating roles on clients as part of the support team",
  )
  val reason: String,

  @Schema(
    required = true,
    description = "The access period required, this can also be used to expire the access if needed",
  )
  val accessPeriod: AccessPeriod,
)

@Schema(description = "Update user's access on allow list")
data class UserAllowlistPatchRequest(
  @Schema(
    required = true,
    description = "A valid business reason for granting access to the environment",
    example = "Access is required to allow updating roles on clients as part of the support team",
  )
  val reason: String,

  @Schema(
    required = true,
    description = "The access period required, this can also be used to expire the access if needed",
  )
  val accessPeriod: AccessPeriod,
)

@Schema(description = "Allow list user details")
data class UserAllowlistDetail(
  @Schema(
    required = true,
    description = "The UUID of the allow list user",
    example = "e287a472-700a-4523-8565-578147667966",
  )
  val id: UUID,

  @Schema(
    required = true,
    description = "NOMIS, nDelius or auth username (can also be an email)",
    examples = ["TD00012", "eliseo.hassen@justice.gov.uk"],
  )
  val username: String,

  @Schema(
    required = true,
    description = "First name",
    example = "Kellianne",
  )
  val firstName: String,

  @Schema(
    required = true,
    description = "Last name",
    example = "Granados",
  )
  val lastName: String,

  @Schema(
    required = true,
    description = "Email",
    example = "kellianne.granados@justice.gov.uk",
  )
  val email: String,

  @Schema(
    required = true,
    description = "A valid business reason for granting access to the environment",
    example = "Access is required to allow updating roles on clients as part of the support team",
  )
  val reason: String,

  @Schema(
    required = true,
    description = "The timestamp the user was added to the allow list",
    example = "04/08/2013T15:53:38.506",
  )
  val createdOn: LocalDateTime,

  @Schema(
    required = true,
    description = "The date the user's access will expire'",
    example = "04/08/2013",
  )
  val allowlistEndDate: LocalDate,

  @Schema(
    required = true,
    description = "The timestamp the allow list user was last updated",
    example = "04/08/2013T15:53:38.506",
  )
  val lastUpdated: LocalDateTime,

  @Schema(
    required = true,
    description = "The logged in username to last update this allow list user",
    example = "SYNDYRBP1",
  )
  val lastUpdatedBy: String,
)

enum class AccessPeriod {
  EXPIRE,
  ONE_MONTH,
  THREE_MONTHS,
  SIX_MONTHS,
  TWELVE_MONTHS,
  NO_RESTRICTION,
}

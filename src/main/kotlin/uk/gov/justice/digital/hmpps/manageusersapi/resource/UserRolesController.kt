package uk.gov.justice.digital.hmpps.manageusersapi.resource

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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserRolesService
import javax.validation.constraints.Size

@RestController
@Validated
@RequestMapping("/users", produces = [MediaType.APPLICATION_JSON_VALUE])
class UserRolesController(
  private val userRolesService: UserRolesService
) {

  @PreAuthorize("hasRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN') or hasRole('ROLE_MAINTAIN_ACCESS_ROLES')")
  @GetMapping("/{username}/roles")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get list of roles associated with the users account",
    description = "Roles for a specific user. Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES",
    security = [SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES"), SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES_ADMIN")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "User role list"
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get caseloads for a user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class)
          )
        ]
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
  fun getUserRoles(
    @Schema(description = "Username", example = "TEST_USER1", required = true)
    @PathVariable @Size(max = 30, min = 1, message = "username must be between 1 and 30") username: String,
  ): UserRoleDetail {
    return userRolesService.getUserRoles(username)
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User & Role Information")
data class UserRoleDetail(
  @Schema(description = "Username", example = "TESTUSER1", required = true) val username: String,
  @Schema(description = "Indicates that the user is active", example = "true", required = true) val active: Boolean,
  @Schema(
    description = "Type of user account",
    example = "GENERAL",
    required = true
  ) val accountType: UsageType = UsageType.GENERAL,
  @Schema(
    description = "Active Caseload of the user",
    example = "BXI",
    required = false
  ) val activeCaseload: PrisonCaseload?,
  @Schema(description = "DPS Roles assigned to this user", required = false) val dpsRoles: List<RoleDetail> = listOf(),
  @Schema(
    description = "NOMIS Roles assigned to this user per caseload",
    required = false
  ) val nomisRoles: List<CaseloadRoleDetail>?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Role Information")
data class RoleDetail(

  @Schema(description = "Role Code", example = "GLOBAL_SEARCH", required = true)
  val code: String,

  @Schema(description = "Role Name", example = "Global Search Role", required = true)
  val name: String,

  @Schema(description = "Role Sequence", example = "1", required = false, defaultValue = "1")
  val sequence: Int = 1,

  @Schema(description = "Role Type ", example = "APP", required = false, defaultValue = "APP")
  val type: RoleType? = RoleType.APP,

  @Schema(description = "Admin only role", example = "true", required = false, defaultValue = "false")
  val adminRoleOnly: Boolean = false,

  @Schema(description = "Parent Role Code", example = "GLOBAL_SEARCH", required = false)
  val parentRole: RoleDetail? = null,
)

enum class RoleType {
  APP, INST, COMM
}

enum class UsageType {
  GENERAL, ADMIN;
}

data class PrisonCaseload(
  @Schema(description = "identify for caseload", example = "WWI")
  val id: String,
  @Schema(description = "name of caseload, typically prison name", example = "WANDSWORTH (HMP)")
  val name: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Roles in caseload information")
data class CaseloadRoleDetail(
  @Schema(description = "Caseload for the listed roles", required = true) val caseload: PrisonCaseload,
  @Schema(description = "NOMIS Roles assigned to this user", required = false) val roles: List<RoleDetail> = listOf(),
)

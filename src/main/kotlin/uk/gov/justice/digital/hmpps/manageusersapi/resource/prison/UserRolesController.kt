package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.DPS_CASELOAD
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseloadRole
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonRole
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserRolesService

@RestController("PrisonUserRolesController")
@Validated
@RequestMapping("/prisonusers", produces = [MediaType.APPLICATION_JSON_VALUE])
class UserRolesController(
  private val userRolesService: UserRolesService,
) {

  @PreAuthorize("hasRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN') or hasRole('ROLE_MAINTAIN_ACCESS_ROLES')")
  @GetMapping("/{username}/roles")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get list of roles associated with the users account",
    description = "Roles for a specific user. Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES",
    security = [SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES"), SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES_ADMIN")],
  )
  @StandardApiResponses
  fun getUserRoles(
    @Schema(description = "Username", example = "TEST_USER1", required = true)
    @PathVariable
    @Size(max = 30, min = 1, message = "username must be between 1 and 30")
    username: String,
  ): UserRoleDetail {
    return UserRoleDetail.fromDomain(userRolesService.getUserRoles(username))
  }

  @PreAuthorize("hasRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN') or hasRole('ROLE_MAINTAIN_ACCESS_ROLES')")
  @PostMapping("/{username}/roles")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Add a role to the specified user account, all roles will be added to DPS caseload unless specified",
    description = "Adds a role to a user, user must have caseload (if specified). Default caseload is DPS caseload (NWEB).  Cannot add an existing role to the same user. Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES",
    security = [SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES"), SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES_ADMIN")],
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "User information with role details",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to add a role to a user account",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to add a role to this account",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun addRoles(
    @Schema(
      description = "Username of the account to add roles",
      example = "TEST_USER2",
      required = true,
    )
    @PathVariable
    @Size(max = 30, min = 1, message = "Username must be between 1 and 30 characters")
    username: String,
    @Schema(description = "Caseload Id", example = "NWEB", required = false, defaultValue = "NWEB")
    @RequestParam(required = false, defaultValue = "NWEB")
    @Size(
      max = 6,
      min = 3,
      message = "Caseload must be between 3-6 characters",
    )
    caseloadId: String = DPS_CASELOAD,
    @Schema(description = "Role Codes", required = true)
    @RequestBody
    @Valid
    roleCodes: List<String>,
  ): UserRoleDetail = userRolesService.addRolesToUser(username, roleCodes, caseloadId)
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User & Role Information")
data class UserRoleDetail(
  @Schema(description = "Username", example = "TESTUSER1", required = true) val username: String,
  @Schema(description = "Indicates that the user is active", example = "true", required = true) val active: Boolean,
  @Schema(
    description = "Type of user account",
    example = "GENERAL",
    required = true,
  ) val accountType: UsageType = UsageType.GENERAL,
  @Schema(
    description = "Active Caseload of the user",
    example = "BXI",
    required = false,
  ) val activeCaseload: PrisonCaseload?,
  @Schema(description = "DPS Roles assigned to this user", required = false) val dpsRoles: List<RoleDetail> = listOf(),
  @Schema(
    description = "NOMIS Roles assigned to this user per caseload",
    required = false,
  ) val nomisRoles: List<CaseloadRoleDetail>?,
) {
  companion object {
    fun fromDomain(prisonUserRole: PrisonUserRole): UserRoleDetail {
      with(prisonUserRole) {
        return UserRoleDetail(
          username,
          active,
          UsageType.valueOf(accountType.name),
          activeCaseload?.let { PrisonCaseload.fromDomain(activeCaseload) },
          dpsRoles.map { RoleDetail.fromDomain(it) },
          nomisRoles?.map { CaseloadRoleDetail.fromDomain(it) },
        )
      }
    }
  }
}

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
) {
  companion object {
    fun fromDomain(prisonRole: PrisonRole): RoleDetail {
      with(prisonRole) {
        val roleType = type?.let { RoleType.valueOf(type.name) } ?: RoleType.APP
        return RoleDetail(code, name, sequence, roleType)
      }
    }
  }
}

enum class RoleType {
  APP,
  INST,
  COMM,
}

enum class UsageType {
  GENERAL,
  ADMIN,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Roles in caseload information")
data class CaseloadRoleDetail(
  @Schema(description = "Caseload for the listed roles", required = true) val caseload: PrisonCaseload,
  @Schema(description = "NOMIS Roles assigned to this user", required = false) val roles: List<RoleDetail> = listOf(),
) {
  companion object {
    fun fromDomain(pcr: PrisonCaseloadRole) =
      CaseloadRoleDetail(PrisonCaseload.fromDomain(pcr.caseload), pcr.roles.map { RoleDetail.fromDomain(it) })
  }
}

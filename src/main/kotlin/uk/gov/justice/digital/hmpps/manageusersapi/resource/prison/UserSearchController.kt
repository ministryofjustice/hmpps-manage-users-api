package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.apache.commons.text.WordUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.*
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.FailApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserService
import uk.gov.justice.digital.hmpps.nomisuserrolesapi.data.filter.PrisonUserFilter

@RestController("PrisonUserSearchController")
@Validated
class UserSearchController(
  private val prisonUserService: UserService,
  private val authenticationFacade: AuthenticationFacade,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) {
  @PreAuthorize("hasRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN') or hasRole('ROLE_MAINTAIN_ACCESS_ROLES')")
  @GetMapping
  @Operation(
    summary = "Get all users filtered as specified",
    description = "Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES. <br/>Get all users with filter.<br/> For local administrators this will implicitly filter users in the prisons they administer, therefore username is expected in the authorisation token. <br/>For users with role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN this allows access to all staff.",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Pageable list of user summaries",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect filter supplied",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getUsers(
    @PageableDefault(sort = ["lastName", "firstName"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "nameFilter", required = false)
    @Parameter(
      description = "Filter results by name (first name and/or last name in any order), username or email address.",
      example = "Raj",
    )
    nameFilter: String?,
    @RequestParam(value = "accessRoles", required = false)
    @Parameter(
      description = "Filter will match users that have all DPS role specified",
      example = "ADD_SENSITIVE_CASE_NOTES",
    )
    accessRoles: List<String>?,
    @RequestParam(value = "nomisRole", required = false)
    @Parameter(
      description = "Filter will match users that have the NOMIS role specified, should be used with a caseloadId or will get duplicates",
      example = "201",
    )
    nomisRole: String?,
    @RequestParam(value = "status", required = false, defaultValue = "ALL")
    @Parameter(
      description = "Limit to active / inactive / show all users",
      example = "INACTIVE",
    )
    status: UserStatus = UserStatus.ACTIVE,
    @Parameter(
      description = "Filter results by user's currently active caseload i.e. the one they have currently selected",
      example = "MDI",
    )
    @RequestParam(value = "activeCaseload", required = false)
    activeCaseload: String?,
    @Parameter(
      description = "Filter results to include only those users that have access to the specified caseload (irrespective of whether it is currently active or not",
      example = "MDI",
    )
    @RequestParam(value = "caseload", required = false)
    caseload: String?,
    @RequestParam(value = "inclusiveRoles", required = false, defaultValue = "false")
    @Parameter(
      description = "Returns result inclusive of selected roles",
      example = "true",
    )
    inclusiveRoles: Boolean = false,
    @RequestParam(value = "showOnlyLSAs", required = false, defaultValue = "false")
    @Parameter(
      description = "Returns all active LSAs",
      example = "true",
    )
    showOnlyLSAs: Boolean = false,
  ): PagedResponse<PrisonUserSummary> = prisonUserService.findUsersByFilter(
    pageRequest,
    PrisonUserFilter(
      localAdministratorUsername = localAdministratorUsernameWhenNotCentralAdministrator(),
      name = nameFilter.nonBlank(),
      status = status,
      activeCaseloadId = activeCaseload.nonBlank(),
      caseloadId = caseload.nonBlank(),
      roleCodes = accessRoles ?: listOf(),
      nomisRoleCode = nomisRole,
      inclusiveRoles = inclusiveRoles,
      showOnlyLSAs = showOnlyLSAs,
    ),
  )

  @GetMapping("/download")
  fun downloadUsersByFilters(
    @PageableDefault(sort = ["lastName", "firstName"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "nameFilter", required = false)
    @Parameter(
      description = "Filter results by name (first name and/or last name in any order), username or email address.",
      example = "Raj",
    )
    nameFilter: String?,
    @RequestParam(value = "accessRoles", required = false)
    @Parameter(
      description = "Filter will match users that have all DPS role specified",
      example = "ADD_SENSITIVE_CASE_NOTES",
    )
    accessRoles: List<String>?,
    @RequestParam(value = "nomisRole", required = false)
    @Parameter(
      description = "Filter will match users that have the NOMIS role specified, should be used with a caseloadId or will get duplicates",
      example = "201",
    )
    nomisRole: String?,
    @RequestParam(value = "status", required = false, defaultValue = "ALL")
    @Parameter(
      description = "Limit to active / inactive / show all users",
      example = "INACTIVE",
    )
    status: UserStatus = UserStatus.ACTIVE,
    @Parameter(
      description = "Filter results by user's currently active caseload i.e. the one they have currently selected",
      example = "MDI",
    )
    @RequestParam(value = "activeCaseload", required = false)
    activeCaseload: String?,
    @Parameter(
      description = "Filter results to include only those users that have access to the specified caseload (irrespective of whether it is currently active or not",
      example = "MDI",
    )
    @RequestParam(value = "caseload", required = false)
    caseload: String?,
    @RequestParam(value = "inclusiveRoles", required = false)
    @Parameter(
      description = "Returns result inclusive of selected roles",
      example = "true",
    )
    inclusiveRoles: Boolean?,
    @RequestParam(value = "showOnlyLSAs", required = false, defaultValue = "false")
    @Parameter(
      description = "Returns all active LSAs",
      example = "true",
    )
    showOnlyLSAs: Boolean = false,
  ): List<PrisonUserSummary> = prisonUserService.downloadUsersByFilter(
    PrisonUserFilter(
      localAdministratorUsername = localAdministratorUsernameWhenNotCentralAdministrator(),
      name = nameFilter.nonBlank(),
      status = status,
      activeCaseloadId = activeCaseload.nonBlank(),
      caseloadId = caseload.nonBlank(),
      roleCodes = accessRoles ?: listOf(),
      nomisRoleCode = nomisRole,
      inclusiveRoles = inclusiveRoles,
      showOnlyLSAs = showOnlyLSAs,
    ),
  )

  @GetMapping("/download/admins")
  fun downloadPrisonAdminsByFilter(
    @PageableDefault(sort = ["lastName", "firstName"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @RequestParam(value = "nameFilter", required = false)
    @Parameter(
      description = "Filter results by name (first name and/or last name in any order), username or email address.",
      example = "Raj",
    )
    nameFilter: String?,
    @RequestParam(value = "accessRoles", required = false)
    @Parameter(
      description = "Filter will match users that have all DPS role specified",
      example = "ADD_SENSITIVE_CASE_NOTES",
    )
    accessRoles: List<String>?,
    @RequestParam(value = "nomisRole", required = false)
    @Parameter(
      description = "Filter will match users that have the NOMIS role specified, should be used with a caseloadId or will get duplicates",
      example = "201",
    )
    nomisRole: String?,
    @RequestParam(value = "status", required = false, defaultValue = "ALL")
    @Parameter(
      description = "Limit to active / inactive / show all users",
      example = "INACTIVE",
    )
    status: UserStatus = UserStatus.ACTIVE,
    @Parameter(
      description = "Filter results by user's currently active caseload i.e. the one they have currently selected",
      example = "MDI",
    )
    @RequestParam(value = "activeCaseload", required = false)
    activeCaseload: String?,
    @Parameter(
      description = "Filter results to include only those users that have access to the specified caseload (irrespective of whether it is currently active or not",
      example = "MDI",
    )
    @RequestParam(value = "caseload", required = false)
    caseload: String?,
    @RequestParam(value = "inclusiveRoles", required = false)
    @Parameter(
      description = "Returns result inclusive of selected roles",
      example = "true",
    )
    inclusiveRoles: Boolean?,
    @RequestParam(value = "showOnlyLSAs", required = false, defaultValue = "false")
    @Parameter(
      description = "Returns all active LSAs",
      example = "true",
    )
    showOnlyLSAs: Boolean = false,
  ): List<PrisonAdminUserSummary> = prisonUserService.downloadPrisonAdminsByFilter(
    PrisonUserFilter(
      localAdministratorUsername = localAdministratorUsernameWhenNotCentralAdministrator(),
      name = nameFilter.nonBlank(),
      status = status,
      activeCaseloadId = activeCaseload.nonBlank(),
      caseloadId = caseload.nonBlank(),
      roleCodes = accessRoles ?: listOf(),
      nomisRoleCode = nomisRole,
      inclusiveRoles = inclusiveRoles,
      showOnlyLSAs = showOnlyLSAs,
    ),
  )

  fun localAdministratorUsernameWhenNotCentralAdministrator(): String? =
    if (AuthenticationFacade.hasRoles("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")) null else authenticationFacade.currentUsername
}

enum class UserStatus {
  ALL,
  ACTIVE,
  INACTIVE,
}

private fun String?.nonBlank() = if (this.isNullOrBlank()) null else this
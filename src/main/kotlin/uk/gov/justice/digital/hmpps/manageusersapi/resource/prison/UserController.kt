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
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonAdminUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonStaffUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUsageType
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserDownloadSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserSearchSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.filter.PrisonUserFilter
import uk.gov.justice.digital.hmpps.manageusersapi.model.filter.PrisonUserStatus
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.FailApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserService

@RestController("PrisonSearchController")
@Validated
class UserSearchController(
  private val prisonUserService: UserService,
  private val authenticationFacade: AuthenticationFacade,
  @Value("\${application.smoketest.enabled}") private val smokeTestEnabled: Boolean,
) {
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN', 'ROLE_MAINTAIN_ACCESS_ROLES', 'ROLE_STAFF_SEARCH')")
  @GetMapping("/prisonusers/search")
  @Operation(
    summary = "Get all users filtered as specified",
    description = "Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN, ROLE_MAINTAIN_ACCESS_ROLES or ROLE_STAFF_SEARCH. <br/>Get all users with filter.<br/> For local administrators this will implicitly filter users in the prisons they administer, therefore username is expected in the authorisation token. <br/>For users with role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN this allows access to all staff.",
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
    status: PrisonUserStatus = PrisonUserStatus.ACTIVE,
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
  ): PagedResponse<PrisonUserSearchSummary> = prisonUserService.findUsersByFilter(
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

  @PreAuthorize("hasRole('USERS__PRISON_USERS__FIND_BY_CASELOAD_AND_ROLE__RO')")
  @GetMapping("/prisonusers/find-by-caseload-and-role")
  @Operation(
    summary = "Get all users filtered by active caseload and role",
    description = "Requires role USERS__PRISON_USERS__FIND_BY_CASELOAD_AND_ROLE__RO<br/>Get all users with active caseload and nomis role.<br/> This search does not limit by user token.",
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
  fun getUsersByCaseloadAndRole(
    @PageableDefault(sort = ["lastName", "firstName"], direction = Sort.Direction.ASC)
    pageRequest: Pageable,
    @Parameter(
      description = "Filter results by user's currently active caseload i.e. the one they have currently selected",
      example = "MDI",
    )
    @RequestParam(value = "activeCaseload", required = true)
    activeCaseload: String,
    @RequestParam(value = "roleCode", required = true)
    @Parameter(
      description = "Filter will match users that have the DPS role specified",
      example = "ADD_SENSITIVE_CASE_NOTES",
    )
    roleCode: String,
    @RequestParam(value = "status", required = false, defaultValue = "ALL")
    @Parameter(
      description = "Limit to active / inactive / show all users",
      example = "INACTIVE",
    )
    status: PrisonUserStatus = PrisonUserStatus.ACTIVE,
    @RequestParam(value = "activeCaseloadOnly", required = false, defaultValue = "true")
    @Parameter(
      description = "If 'activeCaseloadOnly' is provided and True search for users with the target caseloadId " +
        "irrespective of whether it is currently active or not. The default behaviour is to search for users where the " +
        "target caseloadId is currently active.",
    )
    activeCaseloadOnly: Boolean = true,
  ): PagedResponse<PrisonUserSearchSummary> {
    val filter = if (activeCaseloadOnly) {
      // Filter for users with caseload currently active
      PrisonUserFilter(
        status = status,
        activeCaseloadId = activeCaseload,
        roleCodes = listOf(roleCode),
      )
    } else {
      // Filter for users with caseload irrespective of active status
      PrisonUserFilter(
        status = status,
        caseloadId = activeCaseload,
        roleCodes = listOf(roleCode),
      )
    }
    return prisonUserService.findUsersByCaseloadAndRole(pageRequest, filter)
  }

  @PreAuthorize("hasRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN') or hasRole('ROLE_MAINTAIN_ACCESS_ROLES')")
  @GetMapping("/prisonusers/download")
  fun downloadUsersByFilters(
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
    status: PrisonUserStatus = PrisonUserStatus.ACTIVE,
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
  ): List<PrisonUserDownloadSummary> = prisonUserService.downloadUsersByFilter(
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

  @PreAuthorize("hasRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN') or hasRole('ROLE_MAINTAIN_ACCESS_ROLES')")
  @GetMapping("/prisonusers/download/admins")
  fun downloadPrisonAdminsByFilter(
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
    status: PrisonUserStatus = PrisonUserStatus.ACTIVE,
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

  fun localAdministratorUsernameWhenNotCentralAdministrator(): String? = if (AuthenticationFacade.hasRoles("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")) null else authenticationFacade.currentUsername
}

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
    description = "Amend a prison user email address. Requires role MAINTAIN_ACCESS_ROLES_ADMIN",
    security = [SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES_ADMIN")],
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
    @Parameter(description = "The username of the user.", required = true) @PathVariable username: String,
    @Valid @RequestBody amendEmail: AmendEmail,
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
    @RequestBody @Valid createUserRequest: CreateUserRequest,
  ) = NewPrisonUserDto.fromDomain(prisonUserService.createUser(createUserRequest))

  @GetMapping("/prisonusers/{username}", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN', 'ROLE_MAINTAIN_ACCESS_ROLES', 'ROLE_MANAGE_NOMIS_USER_ACCOUNT', 'ROLE_STAFF_SEARCH')")
  @Operation(
    summary = "Get specified user details",
    description = "Information on a specific user. Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN, ROLE_MAINTAIN_ACCESS_ROLES, ROLE_MANAGE_NOMIS_USER_ACCOUNT or ROLE_STAFF_SEARCH",
    security = [
      SecurityRequirement(name = "ROLE_MAINTAIN_ACCESS_ROLES_ADMIN"),
      SecurityRequirement(name = "ROLE_MAINTAIN_ACCESS_ROLES"),
      SecurityRequirement(name = "ROLE_MANAGE_NOMIS_USER_ACCOUNT"),
      SecurityRequirement(name = "ROLE_STAFF_SEARCH"),
    ],
  )
  @StandardApiResponses
  fun findUserByUsername(
    @Parameter(description = "The username of the user.", required = true) @PathVariable username: String,
  ) = prisonUserService.findUserByUsername(username)?.let { NewPrisonUserDto.fromDomain(it) }

  @GetMapping("/prisonusers/{username}/details", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN', 'ROLE_MAINTAIN_ACCESS_ROLES', 'ROLE_MANAGE_NOMIS_USER_ACCOUNT')")
  @Operation(
    summary = "Get specified user details",
    description = "Information on a specific user. Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES or ROLE_MANAGE_NOMIS_USER_ACCOUNT",
    security = [
      SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES_ADMIN"), SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES"),
      SecurityRequirement(
        name = "ROLE_MANAGE_NOMIS_USER_ACCOUNT",
      ),
    ],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "User Information Returned",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get user information",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get a user",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getUserDetails(
    @Schema(description = "Username", example = "testuser1", required = true)
    @PathVariable
    username: String,
  ) = prisonUserService.findUserDetailsByUsername(username)

  @PostMapping("/prisonusers/find-by-usernames")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN', 'ROLE_MAINTAIN_ACCESS_ROLES', 'ROLE_MANAGE_NOMIS_USER_ACCOUNT', 'ROLE_STAFF_SEARCH')")
  @Operation(
    summary = "Find user details by list of usernames.",
    description = "Find user details by list of usernames. Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN, ROLE_MAINTAIN_ACCESS_ROLES, ROLE_MANAGE_NOMIS_USER_ACCOUNT or ROLE_STAFF_SEARCH",
    security = [
      SecurityRequirement(name = "ROLE_MAINTAIN_ACCESS_ROLES_ADMIN"),
      SecurityRequirement(name = "ROLE_MAINTAIN_ACCESS_ROLES"),
      SecurityRequirement(name = "ROLE_MANAGE_NOMIS_USER_ACCOUNT"),
      SecurityRequirement(name = "ROLE_STAFF_SEARCH"),
    ],
  )
  @StandardApiResponses
  fun findUsersByUsernames(
    @Parameter(description = "The list of usernames.", required = true)
    @RequestBody
    usernames: List<String>,
  ): Map<String, NewPrisonUserDto> {
    val users = prisonUserService.findUsersByUsernames(usernames)
    return users.mapValues { NewPrisonUserDto.fromDomain(it.value) }
  }

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
    ],
  )
  fun createLinkedCentralAdminUser(
    @RequestBody @Valid createLinkedCentralAdminUserRequest: CreateLinkedCentralAdminUserRequest,
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
    ],
  )
  fun createLinkedLocalAdminUser(
    @RequestBody @Valid createLinkedLocalAdminUserRequest: CreateLinkedLocalAdminUserRequest,
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
    @RequestBody @Valid createLinkedGeneralUserRequest: CreateLinkedGeneralUserRequest,
  ) = PrisonStaffUserDto.fromDomain(prisonUserService.createLinkedGeneralUser(createLinkedGeneralUserRequest))

  @GetMapping("/prisonusers", produces = [MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize("hasAnyRole('ROLE_USE_OF_FORCE', 'ROLE_STAFF_SEARCH')")
  @Operation(
    summary = "Find prison users by first and last name.",
    description = "Find prison users by first and last name. Requires role ROLE_USE_OF_FORCE or ROLE_STAFF_SEARCH",
    security = [SecurityRequirement(name = "ROLE_USE_OF_FORCE"), SecurityRequirement(name = "ROLE_STAFF_SEARCH")],
  )
  @StandardApiResponses
  fun findUsersByFirstAndLastName(
    @Parameter(
      description = "The first name to match. Case insensitive.",
      required = true,
    ) @RequestParam @NotEmpty firstName: String,
    @Parameter(
      description = "The last name to match. Case insensitive",
      required = true,
    ) @RequestParam @NotEmpty lastName: String,
  ): List<PrisonUserDto> = prisonUserService.findUsersByFirstAndLastName(firstName, lastName).map {
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

  @PreAuthorize("hasRole('ROLE_MANAGE_NOMIS_USER_ACCOUNT')")
  @PutMapping("/prisonusers/{username}/enable-user")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Unlock user account",
    description = "Unlocks the user account. Requires role ROLE_MANAGE_NOMIS_USER_ACCOUNT",
    security = [SecurityRequirement(name = "MANAGE_NOMIS_USER_ACCOUNT")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "User account unlocked",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to unlock user",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to unlock a user",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun enableUser(
    @Schema(description = "Username", example = "testuser1", required = true) @PathVariable @Size(
      max = 30,
      min = 1,
      message = "username must be between 1 and 30",
    ) username: String,
  ) {
    prisonUserService.enableUser(username)
  }

  @PreAuthorize("hasRole('ROLE_MANAGE_NOMIS_USER_ACCOUNT')")
  @PutMapping("/prisonusers/{username}/disable-user")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Lock user account",
    description = "Locks the user account. Requires role ROLE_MANAGE_NOMIS_USER_ACCOUNT",
    security = [SecurityRequirement(name = "MANAGE_NOMIS_USER_ACCOUNT")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "User account locked",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to lock user",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to lock a user",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun disableUser(
    @Schema(description = "Username", example = "testuser1", required = true) @PathVariable @Size(
      max = 30,
      min = 1,
      message = "username must be between 1 and 30",
    ) username: String,
  ) {
    prisonUserService.disableUser(username)
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DPS User creation")
data class CreateUserRequest(
  @Schema(description = "Username", example = "TEST_USER", required = true) @NotBlank val username: String,

  @Schema(
    description = "Email Address",
    example = "test@justice.gov.uk",
    required = true,
  ) @field:Email(message = "Not a valid email address") @NotBlank val email: String,

  @Schema(description = "First name of the user", example = "John", required = true) @NotBlank val firstName: String,

  @Schema(description = "Last name of the user", example = "Smith", required = true) @NotBlank val lastName: String,

  @Schema(description = "The type of user", example = "DPS_LSA", required = true) @NotBlank val userType: UserType,

  @Schema(
    description = "Default caseload (a.k.a Prison ID)",
    example = "BXI",
    required = false,
  ) val defaultCaseloadId: String? = null,
)

enum class UserType {
  DPS_ADM,
  DPS_GEN,
  DPS_LSA,
}

data class PrisonUserDto(
  @Schema(required = true, example = "RO_USER_TEST") val username: String,
  @Schema(required = true, example = "1234564789") val staffId: Long?,
  @Schema(required = false, example = "ryanorton@justice.gov.uk") val email: String?,
  @Schema(required = true, example = "true") val verified: Boolean,
  @Schema(required = true, example = "Ryan") val firstName: String,
  @Schema(required = true, example = "Orton") val lastName: String,
  @Schema(required = true, example = "Ryan Orton") val name: String,
  @Schema(required = false, example = "MDI") val activeCaseLoadId: String?,
)

@Schema(description = "Prison User Created Details")
data class NewPrisonUserDto(
  @Schema(description = "Username", example = "TEST_USER") val username: String,

  @Schema(description = "Email Address", example = "test@justice.gov.uk") val primaryEmail: String?,

  @Schema(description = "First name of the user", example = "John") val firstName: String,

  @Schema(description = "Last name of the user", example = "Smith") val lastName: String,
) {
  companion object {
    fun fromDomain(newPrisonUser: PrisonUser): NewPrisonUserDto {
      with(newPrisonUser) {
        return NewPrisonUserDto(username, email, firstName, lastName)
      }
    }
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Linking a new Central admin account to an existing general user")
data class CreateLinkedCentralAdminUserRequest(
  @Schema(description = "existingUsername", example = "TESTUSER1", required = true) @field:Size(
    max = 30,
    min = 1,
    message = "Username must be between 1 and 30",
  ) @NotBlank val existingUsername: String,

  @Schema(description = "adminUsername", example = "TESTUSER1_ADM", required = true) @field:Size(
    max = 30,
    min = 1,
    message = "Username must be between 1 and 30",
  ) @NotBlank val adminUsername: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Linking a new Local admin account to an existing general user")
data class CreateLinkedLocalAdminUserRequest(
  @Schema(description = "existingUsername", example = "TESTUSER1", required = true) @field:Size(
    max = 30,
    min = 1,
    message = "Username must be between 1 and 30",
  ) @NotBlank val existingUsername: String,

  @Schema(description = "adminUsername", example = "TESTUSER1_ADM", required = true) @field:Size(
    max = 30,
    min = 1,
    message = "Username must be between 1 and 30",
  ) @NotBlank val adminUsername: String,

  @Schema(
    description = "Default local admin group (prison) to manage users",
    example = "MDI",
    required = true,
  ) @field:Size(
    max = 6,
    min = 3,
    message = "Admin group must be between 3-6 characters",
  ) @NotBlank val localAdminGroup: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Linking a new General account to an existing admin user account")
data class CreateLinkedGeneralUserRequest(
  @Schema(description = "existing admin username", example = "TESTUSER1_ADM", required = true) @field:Size(
    max = 30,
    min = 1,
    message = "Admin Username must be between 1 and 30",
  ) @NotBlank val existingAdminUsername: String,

  @Schema(description = "new general username", example = "TESTUSER1_GEN", required = true) @field:Size(
    max = 30,
    min = 1,
    message = "Username must be between 1 and 30",
  ) @NotBlank val generalUsername: String,

  @Schema(
    description = "Default caseload (a.k.a Prison ID), not required for admin accounts",
    example = "BXI",
    required = true,
  ) @field:Size(
    max = 6,
    min = 3,
    message = "Caseload must be between 3-6 characters",
  ) @NotBlank val defaultCaseloadId: String,
)

data class AmendEmail(
  @Schema(
    required = true,
    description = "Email address",
    example = "prison.user@someagency.justice.gov.uk",
  ) @field:NotBlank(message = "Email must not be blank") val email: String?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison Staff Information")
data class PrisonStaffUserDto(
  @Schema(description = "Staff ID", example = "324323", required = true) val staffId: Long,
  @Schema(description = "First name of the user", example = "John", required = true) val firstName: String,
  @Schema(description = "Last name of the user", example = "Smith", required = true) val lastName: String,
  @Schema(description = "Status of staff account", example = "ACTIVE", required = true) val status: String,
  @Schema(
    description = "Email addresses of staff",
    example = "test@test.com",
    required = false,
  ) val primaryEmail: String?,
  @Schema(
    description = "General user account for this staff member",
    required = false,
  ) val generalAccount: UserCaseloadDto?,
  @Schema(
    description = "Admin user account for this staff member",
    required = false,
  ) val adminAccount: UserCaseloadDto?,
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
  @Schema(
    description = "Indicates that the user is active or not",
    example = "true",
    required = true,
  ) val active: Boolean,
  @Schema(
    description = "Type of user account",
    example = "GENERAL",
    required = true,
  ) val accountType: PrisonUsageType = PrisonUsageType.GENERAL,
  @Schema(
    description = "Active Caseload of the user",
    example = "BXI",
    required = false,
  ) val activeCaseload: PrisonCaseloadDto?,
  @Schema(
    description = "Caseloads available for this user",
    required = false,
  ) val caseloads: List<PrisonCaseloadDto>? = listOf(),
) {
  companion object {
    fun fromDomain(userCaseload: UserCaseload) = UserCaseloadDto(
      userCaseload.username,
      userCaseload.active,
      userCaseload.accountType,
      userCaseload.activeCaseload?.let { PrisonCaseloadDto.fromDomain(it) },
      userCaseload.caseloads?.map { PrisonCaseloadDto.fromDomain(it) },
    )
  }
}

data class PrisonCaseloadDto(
  @Schema(description = "ID for the caseload", example = "WWI") val id: String,
  @Schema(description = "name of caseload, typically prison name", example = "WANDSWORTH (HMP)") val name: String,
) {
  companion object {
    fun fromDomain(pcd: PrisonCaseload) = PrisonCaseloadDto(pcd.id, pcd.name)
  }
}

enum class UserStatus {
  ALL,
  ACTIVE,
  INACTIVE,
}

private fun String?.nonBlank() = if (this.isNullOrBlank()) null else this

package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminTypeReturn
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleExistsException
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.RolesService
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

@Validated
@RestController
class RolesController(
  private val rolesService: RolesService
) {

  @PostMapping("/roles")
  @PreAuthorize("hasRole('ROLE_ROLES_ADMIN')")
  @Operation(
    summary = "Create role",
    description = "Create a new role, role required is ROLE_ROLES_ADMIN",
    security = [SecurityRequirement(name = "ROLE_ROLES_ADMIN")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json")]
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Role Created",
        content = [Content(mediaType = "application/json")]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_ROLES_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  @ResponseStatus(HttpStatus.CREATED)
  @Throws(RoleExistsException::class, RoleNotFoundException::class)
  fun createRole(
    @Schema(description = "Details of the role to be created.", required = true)
    @Valid @RequestBody createRole: CreateRole,
  ) {
    rolesService.createRole(createRole)
  }

  @PreAuthorize("hasRole('ROLE_ROLES_ADMIN')")
  @Operation(
    summary = "Get role details",
    description = "Get role details, role required is ROLE_ROLES_ADMIN",
    security = [SecurityRequirement(name = "ROLE_ROLES_ADMIN")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Role Details Returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = Role::class))
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_ROLES_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  @GetMapping("/roles/{role}")
  fun getRoleDetail(
    @Schema(description = "The Role code of the role.", example = "AUTH_GROUP_MANAGER", required = true)
    @PathVariable role: String,
  ): Role {
    return Role(rolesService.getRoleDetail(role))
  }

  @PreAuthorize("hasRole('ROLE_ROLES_ADMIN')")
  @Operation(
    summary = "Amend role name",
    description = "Amend the role name, role required is ROLE_ROLES_ADMIN",
    security = [SecurityRequirement(name = "ROLE_ROLES_ADMIN")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Role name updated"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_ROLES_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "The role trying to update does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  @PutMapping("/roles/{roleCode}")
  fun amendRoleName(
    @Schema(description = "The Role code of the role.", example = "AUTH_GROUP_MANAGER", required = true)
    @PathVariable roleCode: String,
    @Valid @RequestBody roleAmendment: RoleNameAmendment
  ) {
    rolesService.updateRoleName(roleCode, roleAmendment)
  }

  @PreAuthorize("hasRole('ROLE_ROLES_ADMIN')")
  @Operation(
    summary = "Amend role description",
    description = "Amend the role description, role required is ROLE_ROLES_ADMIN",
    security = [SecurityRequirement(name = "ROLE_ROLES_ADMIN")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Role description updated"
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_ROLES_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "404",
        description = "The role trying to update does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  @PutMapping("/roles/{roleCode}/description")
  fun amendRoleDescription(
    @Schema(description = "The Role code of the role.", example = "AUTH_GROUP_MANAGER", required = true)
    @PathVariable roleCode: String,
    @Valid @RequestBody roleAmendment: RoleDescriptionAmendment
  ) {
    rolesService.updateRoleDescription(roleCode, roleAmendment)
  }
}

data class CreateRole(
  @Schema(required = true, description = "Role Code", example = "AUTH_GROUP_MANAGER")
  @field:NotBlank(message = "role code must be supplied")
  @field:Size(min = 2, max = 30, message = "Role code must be between 2 and 30 characters")
  @field:Pattern(regexp = "^[0-9A-Za-z_]*", message = "Role code must only contain 0-9, A-Z, a-z and _  characters")
  val roleCode: String,

  @Schema(required = true, description = "roleName", example = "Auth Group Manager")
  @field:NotBlank(message = "role name must be supplied")
  @field:Size(min = 4, max = 128, message = "Role name must be between 4 and 100 characters")
  @field:Pattern(regexp = "^[0-9A-Za-z- ,.()'&]*\$", message = "Role name must only contain 0-9, A-Z, a-z and ( ) & , - . '  characters")
  val roleName: String,

  @Schema(
    required = false,
    description = "roleDescription",
    example = "Allow Group Manager to administer the account within their groups",
  )
  @field:Size(max = 1024, message = "Role description must be no more than 1024 characters")
  @field:Pattern(
    regexp = "^[0-9A-Za-z- ,.()'&\r\n]*\$",
    message = "Role description must only contain can only contain 0-9, A-Z, a-z, newline and ( ) & , - . '  characters"
  )
  val roleDescription: String? = null,

  @Schema(
    required = true,
    description = "adminType, can be used if multiple admin types required",
    example = "[\"EXT_ADM\", \"DPS_ADM\"]",
  )
  @field:NotEmpty(message = "Admin type cannot be empty")
  val adminType: Set<AdminType>,
)

@Schema(description = "Role Details")
data class Role(
  @Schema(required = true, description = "Role Code", example = "AUTH_GROUP_MANAGER")
  val roleCode: String,

  @Schema(required = true, description = "Role Name", example = "Auth Group Manager")
  val roleName: String,

  @Schema(
    required = true,
    description = "Role Description",
    example = "Allow Group Manager to administer the account within their groups"
  )
  val roleDescription: String?,

  @Schema(required = true, description = "Administration Type")
  val adminType: List<AdminTypeReturn>,
) {
  constructor(r: Role) : this(
    r.roleCode,
    r.roleName,
    r.roleDescription,
    r.adminType
  )
}

@Schema(description = "Update Role Name")
data class RoleNameAmendment(
  @Schema(required = true, description = "Role Name", example = "Auth Group Manager")
  @field:NotBlank(message = "Role name must be supplied")
  @field:Size(min = 4, max = 100, message = "Role name must be between 4 and 100 characters")
  @field:Pattern(
    regexp = "^[0-9A-Za-z- ,.()'&]*\$",
    message = "Role name must only contain 0-9, a-z and ( ) & , - . '  characters"
  )
  val roleName: String
)

@Schema(description = "Update Role Description")
data class RoleDescriptionAmendment(
  @Schema(
    required = true, description = "Role Description",
    example = "Allow Group Manager to administer the account within their groups"
  )
  @field:Size(max = 1024, message = "Role description must be no more than 1024 characters")
  @field:Pattern(
    regexp = "^[0-9A-Za-z- ,.()'&\r\n]*\$",
    message = "Role description must only contain can only contain 0-9, a-z, newline and ( ) & , - . '  characters"
  )
  val roleDescription: String?
)

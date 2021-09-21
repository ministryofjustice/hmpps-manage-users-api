package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminTypeReturn
import uk.gov.justice.digital.hmpps.manageusersapi.service.RolesService

@Validated
@RestController
class RolesController(
  private val rolesService: RolesService
) {

  @PreAuthorize("hasRole('ROLE_ROLES_ADMIN')")
  @Operation(
    summary = "Get all audit events",
    description = "Get all audit events, role required is ROLE_AUDIT",
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
}

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

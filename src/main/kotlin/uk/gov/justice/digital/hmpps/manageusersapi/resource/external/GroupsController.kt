package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.ChildGroup
import uk.gov.justice.digital.hmpps.manageusersapi.model.Group
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserAssignableRole
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.FailApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.GroupsService

@Validated
@RestController
class GroupsController(
  private val groupsService: GroupsService,
) {

  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Get all groups",
    description = "Get all groups, role required is ROLE_MAINTAIN_OAUTH_USERS or ROLE_AUTH_GROUP_MANAGER",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS, ROLE_AUTH_GROUP_MANAGER")],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "All Groups Returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = UserGroupDto::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping("/groups")
  fun getGroups(): List<UserGroupDto> = groupsService.getGroups().map { UserGroupDto.fromDomain(it) }

  @PreAuthorize("hasRole('ROLE_CONTRACT_MANAGER_VIEW_GROUP')")
  @Operation(
    summary = "Get the subset of groups that are CRS groups.",
    description = "Get all CRS groups. Requires role ROLE_CONTRACT_MANAGER_VIEW_GROUP",
    security = [SecurityRequirement(name = "ROLE_CONTRACT_MANAGER_VIEW_GROUP")],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "All CRS Groups Returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = UserGroupDto::class)),
          ),
        ],
      ),
    ],
  )
  @GetMapping("/groups/subset/crs")
  fun getCRSGroups(): List<UserGroupDto> = groupsService.getCRSGroups().map { UserGroupDto.fromDomain(it) }

  @GetMapping("/groups/{group}")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_OAUTH_USERS', 'ROLE_AUTH_GROUP_MANAGER')")
  @Operation(
    summary = "Group detail.",
    description = "return Group Details",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS"), SecurityRequirement(name = "ROLE_AUTH_GROUP_MANAGER")],
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getGroupDetail(
    @Parameter(description = "The group code of the group.", required = true)
    @PathVariable
    group: String,
  ): GroupDetailsDto = GroupDetailsDto.fromDomain(groupsService.getGroupDetail(group))

  @GetMapping("/groups/child/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Child Group detail.",
    description = "Fetches child group details.<br/>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS")],
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Child Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getChildGroupDetail(
    @Parameter(description = "The group code of the child group.", required = true)
    @PathVariable
    group: String,
  ): ChildGroupDetailsDto = ChildGroupDetailsDto.fromDomain(groupsService.getChildGroupDetail(group))

  @PutMapping("/groups/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Amend group name.",
    description = "Amend group name.<br/>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupAmendmentDto::class))],
    ),
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun amendGroupName(
    @Parameter(description = "The group code of the group.", required = true)
    @PathVariable
    group: String,
    @Parameter(
      description = "Details of the group to be updated.",
      required = true,
    ) @Valid @RequestBody
    groupAmendment: GroupAmendmentDto,
  ) = groupsService.updateGroup(group, groupAmendment)

  @PutMapping("/groups/child/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Amend child group name.",
    description = "Amend a Child Group Name.<br/>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupAmendmentDto::class))],
    ),
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Child Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun amendChildGroupName(
    @Parameter(description = "The group code of the child group.", required = true)
    @PathVariable
    group: String,
    @Parameter(
      description = "Details of the child group to be updated.",
      required = true,
    ) @Valid @RequestBody
    groupAmendment: GroupAmendmentDto,
  ) = groupsService.updateChildGroup(group, groupAmendment)

  @PostMapping("/groups")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Create group.",
    description = "Create a group.<br/>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CreateGroupDto::class))],
    ),
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "409",
        description = "Group already exists.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createGroup(
    @Schema(description = "Details of the group to be created.", required = true)
    @Valid
    @RequestBody
    createGroup: CreateGroupDto,
  ) = groupsService.createGroup(createGroup)

  @PostMapping("/groups/child")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Create child group.",
    description = "Create a Child Group.<br/>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CreateChildGroupDto::class))],
    ),
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "409",
        description = "Child Group already exists.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun createChildGroup(
    @Schema(description = "Details of the child group to be created.", required = true)
    @Valid
    @RequestBody
    createChildGroup: CreateChildGroupDto,
  ) = groupsService.createChildGroup(createChildGroup)

  @DeleteMapping("/groups/child/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Delete child group.",
    description = "Delete a child group.<br/>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS")],
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Child Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteChildGroup(
    @Parameter(description = "The group code of the child group.", required = true)
    @PathVariable
    group: String,
  ) = groupsService.deleteChildGroup(group)

  @DeleteMapping("/groups/{group}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_OAUTH_USERS')")
  @Operation(
    summary = "Delete group.",
    description = "Delete a Group.<br.>" +
      "Requires role ROLE_MAINTAIN_OAUTH_USERS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_OAUTH_USERS")],
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "Group not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun deleteGroup(
    @Schema(description = "The group code of the group.", required = true)
    @PathVariable
    group: String,
  ) = groupsService.deleteGroup(group)
}

@Schema(description = "Group Details")
data class ChildGroupDetailsDto(
  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  val groupName: String,
) {
  companion object {
    fun fromDomain(childGroupDetails: ChildGroup) = ChildGroupDetailsDto(
      childGroupDetails.groupCode,
      childGroupDetails.groupName,
    )
  }
}

@Schema(description = "Group Name")
data class GroupAmendmentDto(
  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  @field:NotBlank(message = "parent group code must be supplied")
  @field:Size(min = 4, max = 100)
  val groupName: String,
)

@Schema(description = "User Role")
data class UserAssignableRoleDto(
  @Schema(required = true, description = "Role Code", example = "LICENCE_RO")
  val roleCode: String,

  @Schema(required = true, description = "Role Name", example = "Licence Responsible Officer")
  val roleName: String,

  @Schema(required = true, description = "automatic", example = "TRUE")
  val automatic: Boolean,
) {
  companion object {
    fun fromDomain(userAssignableRole: UserAssignableRole) = UserAssignableRoleDto(
      userAssignableRole.roleCode,
      userAssignableRole.roleName,
      userAssignableRole.automatic,
    )
  }
}

@Schema(description = "User Group")
data class UserGroupDto(
  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  val groupName: String,
) {
  companion object {
    fun fromDomain(userGroup: UserGroup) = UserGroupDto(userGroup.groupCode, userGroup.groupName)
  }
}

data class CreateGroupDto(
  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  @field:NotBlank(message = "group code must be supplied")
  @field:Size(min = 2, max = 30)
  @field:Pattern(regexp = "^[0-9A-Za-z_]*")
  val groupCode: String,

  @Schema(required = true, description = "groupName", example = "HDC NPS North East")
  @field:NotBlank(message = "group name must be supplied")
  @field:Size(min = 4, max = 100)
  @field:Pattern(regexp = "^[0-9A-Za-z- ,.()'&]*\$")
  val groupName: String,
)

@Schema(description = "Group Details")
data class GroupDetailsDto(
  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  val groupCode: String,

  @Schema(required = true, description = "Group Name", example = "HDC NPS North East")
  val groupName: String,

  @Schema(required = true, description = "Assignable Roles")
  val assignableRoles: List<UserAssignableRoleDto>,

  @Schema(required = true, description = "Child Groups")
  val children: List<UserGroupDto>,
) {
  companion object {
    fun fromDomain(groupDetails: Group): GroupDetailsDto {
      with(groupDetails) {
        return GroupDetailsDto(
          groupCode,
          groupName,
          assignableRoles.map { UserAssignableRoleDto.fromDomain(it) },
          children.map { UserGroupDto.fromDomain(it) },
        )
      }
    }
  }
}

data class CreateChildGroupDto(
  @Schema(required = true, description = "Parent Group Code", example = "HNC_NPS")
  @field:NotBlank(message = "parent group code must be supplied")
  @field:Size(min = 2, max = 30)
  @field:Pattern(regexp = "^[0-9A-Za-z_]*")
  val parentGroupCode: String,

  @Schema(required = true, description = "Group Code", example = "HDC_NPS_NE")
  @field:NotBlank(message = "group code must be supplied")
  @field:Size(min = 2, max = 30)
  @field:Pattern(regexp = "^[0-9A-Za-z_]*")
  val groupCode: String,

  @Schema(required = true, description = "groupName", example = "HDC NPS North East")
  @field:NotBlank(message = "group name must be supplied")
  @field:Size(min = 4, max = 100)
  @field:Pattern(regexp = "^[0-9A-Za-z- ,.()'&]*\$")
  val groupName: String,
)

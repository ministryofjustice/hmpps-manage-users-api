package uk.gov.justice.digital.hmpps.manageusersapi.model

data class GroupDetails(
  val groupCode: String,
  val groupName: String,
  val assignableRoles: List<UserAssignableRole>,
  val children: List<UserGroup>
)

package uk.gov.justice.digital.hmpps.manageusersapi.model

data class Group(
  val groupCode: String,
  val groupName: String,
  val assignableRoles: List<UserAssignableRole>,
  val children: List<UserGroup>
)

package uk.gov.justice.digital.hmpps.manageusersapi.model

data class UserAssignableRole(
  val roleCode: String,
  val roleName: String,
  val automatic: Boolean,
)

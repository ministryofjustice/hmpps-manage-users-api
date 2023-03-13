package uk.gov.justice.digital.hmpps.manageusersapi.model

data class EnabledExternalUser(
  val username: String,
  val firstName: String,
  val email: String?,
  val admin: String,
)

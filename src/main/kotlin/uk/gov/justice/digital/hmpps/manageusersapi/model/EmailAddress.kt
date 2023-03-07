package uk.gov.justice.digital.hmpps.manageusersapi.model

data class EmailAddress(
  val username: String,
  val email: String?,
  val verified: Boolean
)

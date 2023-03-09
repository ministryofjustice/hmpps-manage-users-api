package uk.gov.justice.digital.hmpps.manageusersapi.model

data class NewPrisonUser(
  val username: String,
  val primaryEmail: String,
  val firstName: String,
  val lastName: String,
)

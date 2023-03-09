package uk.gov.justice.digital.hmpps.manageusersapi.model

data class PrisonUser(
  val username: String,
  val userId: String,
  val email: String?,
  val verified: Boolean,
  val firstName: String,
  val lastName: String,
  val activeCaseLoadId: String?,
)

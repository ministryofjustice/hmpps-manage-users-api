package uk.gov.justice.digital.hmpps.manageusersapi.model

data class PrisonUserSummary(
  val username: String,
  val staffId: String,
  val firstName: String,
  val lastName: String,
  val active: Boolean,
  val activeCaseload: PrisonCaseload?,
  val email: String?,
)

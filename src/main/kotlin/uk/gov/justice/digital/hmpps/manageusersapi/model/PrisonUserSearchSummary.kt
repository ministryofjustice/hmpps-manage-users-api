package uk.gov.justice.digital.hmpps.manageusersapi.model

data class PrisonUserSearchSummary(
  val username: String,
  val staffId: Int,
  val firstName: String,
  val lastName: String,
  val active: Boolean,
  val status: String?,
  val locked: Boolean,
  val expired: Boolean,
  val activeCaseload: PrisonCaseload?,
  val dpsRoleCount: Int,
  val email: String?,
  val staffStatus: String,
)

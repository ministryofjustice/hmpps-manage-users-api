package uk.gov.justice.digital.hmpps.manageusersapi.model

data class PrisonStaffUser(
  val staffId: Long,
  val firstName: String,
  val lastName: String,
  val status: String,
  val primaryEmail: String?,
  val generalAccount: UserCaseload?,
  val adminAccount: UserCaseload?,
)

data class UserCaseload(
  val username: String,
  val active: Boolean,
  val accountType: PrisonUsageType = PrisonUsageType.GENERAL,
  val activeCaseload: PrisonCaseload?,
  val caseloads: List<PrisonCaseload>? = listOf(),
)

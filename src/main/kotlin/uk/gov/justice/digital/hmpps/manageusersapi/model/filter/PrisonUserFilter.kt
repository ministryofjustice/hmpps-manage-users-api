package uk.gov.justice.digital.hmpps.manageusersapi.model.filter

data class PrisonUserFilter(
  val localAdministratorUsername: String? = null,
  val name: String? = null,
  val status: PrisonUserStatus? = null,
  val activeCaseloadId: String? = null,
  val caseloadId: String? = null,
  val roleCodes: List<String> = listOf(),
  val nomisRoleCode: String? = null,
  val inclusiveRoles: Boolean? = null,
  val showOnlyLSAs: Boolean? = false,
)

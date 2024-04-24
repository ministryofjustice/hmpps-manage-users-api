package uk.gov.justice.digital.hmpps.manageusersapi.model

import com.fasterxml.jackson.annotation.JsonInclude

const val DPS_CASELOAD = "NWEB"
const val CENTRAL_ADMIN_CASELOAD = "CADM_I"

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonUserRole(
  val username: String,
  val active: Boolean,
  val accountType: PrisonUsageType = PrisonUsageType.GENERAL,
  val activeCaseload: PrisonCaseload?,
  val dpsRoles: List<PrisonRole> = listOf(),
  val nomisRoles: List<PrisonCaseloadRole>?,
)

data class PrisonCaseload(
  val id: String,
  val name: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonCaseloadRole(
  val caseload: PrisonCaseload,
  val roles: List<PrisonRole> = listOf(),
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PrisonRole(
  val code: String,
  val name: String,
  val sequence: Int = 1,
  val type: PrisonRoleType? = PrisonRoleType.APP,
  val adminRoleOnly: Boolean = false,
  val parentRole: PrisonRole? = null,
)

enum class PrisonRoleType {
  APP,
  INST,
  COMM,
}

enum class PrisonUsageType {
  GENERAL,
  ADMIN,
}

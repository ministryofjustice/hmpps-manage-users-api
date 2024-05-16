package uk.gov.justice.digital.hmpps.manageusersapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Summary User Information with Email Address")
data class PrisonAdminUserSummary(
  val username: String,
  val staffId: Long,
  val firstName: String,
  val lastName: String,
  val active: Boolean,
  val status: PrisonAccountStatus?,
  val locked: Boolean = false,
  val expired: Boolean = false,
  val activeCaseload: PrisonCaseload?,
  val dpsRoleCount: Int,
  val email: String?,
  val groups: List<PrisonUserGroupDetail>,
  val staffStatus: String?,
)

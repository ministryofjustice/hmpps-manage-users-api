package uk.gov.justice.digital.hmpps.manageusersapi.model

class Role(
  val roleCode: String,
  val roleName: String,
  val roleDescription: String?,
  val adminType: List<AdminTypeReturn>,
)

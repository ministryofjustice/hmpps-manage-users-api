package uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.model

data class AuthService(
  val code: String,
  val name: String,
  val description: String?,
  val contact: String?,
  val url: String?,
)

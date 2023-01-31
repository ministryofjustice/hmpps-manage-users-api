package uk.gov.justice.digital.hmpps.manageusersapi.model

interface UserDetails {
  val userId: String
  val name: String
  val firstName: String
  val authSource: String
  fun toUserDetails(): UserDetailsDto
}

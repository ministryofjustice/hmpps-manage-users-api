package uk.gov.justice.digital.hmpps.manageusersapi.model

data class DeliusUserDetails(
  private val username: String,
  override val userId: String,
  override val firstName: String,
  val surname: String,
  val email: String,
  private val enabled: Boolean = false,
) : UserDetails {

  override val name: String
    get() = "$firstName $surname"

  override val isAdmin: Boolean = false

  override val authSource: String
    get() = "delius"

  fun toUserDetails(): UserDetailsDto =
    UserDetailsDto(
      username = username,
      active = enabled,
      authSource = AuthSource.delius,
      name = "$firstName $surname",
      userId = userId,
      uuid = null
    )
}

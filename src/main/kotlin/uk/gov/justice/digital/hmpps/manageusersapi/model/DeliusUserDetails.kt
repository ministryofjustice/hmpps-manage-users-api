package uk.gov.justice.digital.hmpps.manageusersapi.model

data class DeliusUserDetails(
  private val username: String,
  override val userId: String,
  override val firstName: String,
  val surname: String,
  private val enabled: Boolean = false,
) : UserDetails {

  override val name: String
    get() = "$firstName $surname"

  override val authSource: String
    get() = "delius"

  override fun toUserDetails(): UserDetailsDto =
    UserDetailsDto(
      username = username,
      active = enabled,
      authSource = AuthSource.delius,
      name = name,
      userId = userId,
      uuid = null
    )
}

package uk.gov.justice.digital.hmpps.manageusersapi.model

data class DeliusUser(
  private val username: String,
  override val userId: String,
  override val firstName: String,
  val surname: String,
  private val enabled: Boolean = false,
) : UserDetails, SourceUser {

  override val name: String
    get() = "$firstName $surname"

  override val authSource: String
    get() = "delius"

  override fun toUserDetails(): UserDetailsDto =
    UserDetailsDto(
      username = username.uppercase(),
      active = enabled,
      authSource = AuthSource.delius,
      name = name,
      userId = userId,
      uuid = null
    )

  override fun toGenericUser(): GenericUser =
    GenericUser(
      username = username.uppercase(),
      active = enabled,
      authSource = AuthSource.delius,
      name = name,
      userId = userId,
      uuid = null,
    )
}

package uk.gov.justice.digital.hmpps.manageusersapi.model

data class DeliusUser(
  private val username: String,
  val userId: String,
  val firstName: String,
  val surname: String,
  private val enabled: Boolean = false,
) : SourceUser {

  val name: String
    get() = "$firstName $surname"

  val authSource: String
    get() = "delius"

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

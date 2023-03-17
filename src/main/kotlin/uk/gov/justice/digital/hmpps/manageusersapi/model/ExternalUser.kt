package uk.gov.justice.digital.hmpps.manageusersapi.model

import java.time.LocalDateTime
import java.util.UUID

data class ExternalUser(
  val userId: UUID,
  override val username: String,
  val email: String,
  override val firstName: String,
  override val lastName: String,
  val locked: Boolean = false,
  val enabled: Boolean = false,
  val verified: Boolean = false,
  val lastLoggedIn: LocalDateTime? = null,
  val inactiveReason: String? = null,
) : SourceUser, UserIdentity {

  override fun toGenericUser(): GenericUser =
    GenericUser(
      username = username,
      active = enabled,
      authSource = AuthSource.auth,
      name = "$firstName $lastName",
      userId = userId.toString(),
      uuid = userId,
    )
}

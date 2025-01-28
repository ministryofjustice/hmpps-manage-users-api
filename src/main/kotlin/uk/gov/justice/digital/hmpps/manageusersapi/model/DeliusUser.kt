package uk.gov.justice.digital.hmpps.manageusersapi.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.delius

data class DeliusUser(
  val username: String,
  val userId: String,
  val firstName: String,
  val surname: String,
  val enabled: Boolean = false,
  val email: String,
  val roles: List<String> = emptyList(),
) : SourceUser {

  val name: String
    get() = "$firstName $surname"

  override val authSource: AuthSource
    get() = delius

  override fun toGenericUser(): GenericUser = GenericUser(
    username = username.uppercase(),
    active = enabled,
    authSource = delius,
    name = name,
    userId = userId,
    uuid = null,
    roles = roles.map { UserRole(it) },
  )

  override fun emailAddress(): EmailAddress = EmailAddress(username, email, true)
}

data class UserRole @JsonCreator constructor(@JsonProperty("name") val name: String)

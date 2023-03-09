package uk.gov.justice.digital.hmpps.manageusersapi.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class DeliusUser(
  private val username: String,
  val userId: String,
  val firstName: String,
  val surname: String,
  private val enabled: Boolean = false,
  val email: String,
  val roles: List<UserRole>? = emptyList(),
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
      roles = roles,
    )
}

data class UserRole @JsonCreator constructor(@JsonProperty("name") val name: String)

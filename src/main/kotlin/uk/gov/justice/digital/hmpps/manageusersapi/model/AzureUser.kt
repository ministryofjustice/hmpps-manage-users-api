package uk.gov.justice.digital.hmpps.manageusersapi.model

import java.util.UUID

data class AzureUser(
  val username: String,
  var active: Boolean,
  var name: String,
  var authSource: AuthSource,
  var userId: String,
  var uuid: UUID? = null
) : SourceUser {
  override fun toGenericUser(): GenericUser =
    GenericUser(
      username = username,
      active = active,
      authSource = authSource,
      name = name,
      userId = userId,
      uuid = uuid,
    )
}

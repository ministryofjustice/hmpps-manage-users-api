package uk.gov.justice.digital.hmpps.manageusersapi.model

import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.azuread

data class AzureUser(
  val username: String,
  var enabled: Boolean,
  var firstName: String,
  var lastName: String,
  var email: String,
  var roles: List<UserRole> = emptyList(),
) : SourceUser {
  override val authSource: AuthSource
    get() = azuread

  override fun toGenericUser(): GenericUser =
    GenericUser(
      username = username,
      active = enabled,
      authSource = azuread,
      name = "$firstName $lastName",
      userId = email,
      uuid = null,
    )

  override fun emailAddress(): EmailAddress =
    EmailAddress(username, email, true)
}

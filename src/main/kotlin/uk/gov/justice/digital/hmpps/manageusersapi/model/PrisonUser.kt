package uk.gov.justice.digital.hmpps.manageusersapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.nomis

data class PrisonUser(
  override val username: String,
  override val firstName: String,
  val staffId: Int,
  override val lastName: String,
  @JsonProperty("activeCaseloadId") val activeCaseLoadId: String?,
  @JsonProperty("primaryEmail") val email: String?,
  private val enabled: Boolean = false,
  @JsonProperty("dpsRoleCodes") val roles: List<String> = emptyList(),
) : SourceUser,
  UserIdentity {
  val userId = staffId

  val name: String
    get() = "$firstName $lastName"

  override val authSource: AuthSource
    get() = nomis

  override fun toGenericUser(): GenericUser = GenericUser(
    username = username,
    active = enabled,
    authSource = nomis,
    userId = userId.toString(),
    name = name,
    uuid = null,
    staffId = userId.toLong(),
    activeCaseLoadId = activeCaseLoadId,
  )

  override fun emailAddress(): EmailAddress = EmailAddress(username, email, !email.isNullOrEmpty())
}

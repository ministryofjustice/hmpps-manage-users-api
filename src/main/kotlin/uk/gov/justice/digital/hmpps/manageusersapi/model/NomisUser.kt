package uk.gov.justice.digital.hmpps.manageusersapi.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NomisUser(
  private val username: String,
  val firstName: String,
  val staffId: String,
  val lastName: String,
  @JsonProperty("activeCaseloadId") val activeCaseLoadId: String?,
  @JsonProperty("primaryEmail") val email: String?,
  private val enabled: Boolean = false,
) : SourceUser {
  val userId = staffId

  val name: String
    get() = "$firstName $lastName"

  val authSource: String
    get() = "nomis"

  override fun toGenericUser(): GenericUser =
    GenericUser(
      username = username,
      active = enabled,
      authSource = AuthSource.nomis,
      userId = userId,
      name = name,
      uuid = null,
      staffId = userId.toLong(),
      activeCaseLoadId = activeCaseLoadId
    )
}

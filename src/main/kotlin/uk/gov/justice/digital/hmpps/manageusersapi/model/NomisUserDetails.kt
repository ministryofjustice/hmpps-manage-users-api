package uk.gov.justice.digital.hmpps.manageusersapi.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NomisUserDetails(
  private val username: String,
  override val firstName: String,
  val staffId: String,
  val lastName: String,
  @JsonProperty("activeCaseloadId") val activeCaseLoadId: String?,
  @JsonProperty("primaryEmail") val email: String?,
  private val enabled: Boolean = false,
) : UserDetails {
  override val userId = staffId

  override val name: String
    get() = "$firstName $lastName"

  override val authSource: String
    get() = "nomis"

  override fun toUserDetails(): UserDetailsDto =
    UserDetailsDto(
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

package uk.gov.justice.digital.hmpps.manageusersapi.fixtures

import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.NomisUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import java.time.LocalDateTime
import java.util.UUID

class UserFixture {

  companion object {
    fun createExternalUserDetails(
      userId: UUID,
      username: String = "testing",
      email: String = "testy@testing.com",
      firstName: String = "first",
      lastName: String = "last",
      locked: Boolean = false,
      enabled: Boolean = true,
      verified: Boolean = true,
      lastLoggedIn: LocalDateTime = LocalDateTime.now().minusDays(1),
      inactiveReason: String? = null,
    ) = ExternalUser(
      userId = userId,
      username = username,
      email = email,
      firstName = firstName,
      lastName = lastName,
      locked = locked,
      enabled = enabled,
      verified = verified,
      lastLoggedIn = lastLoggedIn,
      inactiveReason = inactiveReason,
    )

    fun createNomisUserDetails(
      username: String = "NUSER_GEN",
      firstName: String = "Nomis",
      staffId: Int = 123456,
      lastName: String = "Take",
      activeCaseLoadId: String = "MDI",
      email: String = "nomis.usergen@digital.justice.gov.uk",
      enabled: Boolean = true,
      roles: List<String> = listOf("ROLE1", "ROLE2", "ROLE3"),

    ) = NomisUser(
      username = username,
      firstName = firstName,
      staffId = staffId,
      lastName = lastName,
      activeCaseLoadId = activeCaseLoadId,
      email = email,
      enabled = enabled,
      roles = roles,
    )
  }
}

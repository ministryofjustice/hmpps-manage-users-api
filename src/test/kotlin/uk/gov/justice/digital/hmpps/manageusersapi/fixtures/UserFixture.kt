package uk.gov.justice.digital.hmpps.manageusersapi.fixtures

import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
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
      inactiveReason: String? = null
    ): ExternalUserDetailsDto {
      return ExternalUserDetailsDto(
        userId = userId,
        username = username,
        email = email,
        firstName = firstName,
        lastName = lastName,
        locked = locked,
        enabled = enabled,
        verified = verified,
        lastLoggedIn = lastLoggedIn,
        inactiveReason = inactiveReason
      )
    }
  }
}

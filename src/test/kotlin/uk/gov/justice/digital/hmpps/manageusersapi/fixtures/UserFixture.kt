package uk.gov.justice.digital.hmpps.manageusersapi.fixtures

import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import java.time.LocalDateTime
import java.util.UUID

class UserFixture {

  fun createExternalUserDetails(userId: UUID, username: String, email: String): ExternalUserDetailsDto {
    return ExternalUserDetailsDto(
      userId = userId,
      username = username,
      email = email,
      firstName = "first",
      lastName = "last",
      locked = false,
      enabled = true,
      verified = true,
      lastLoggedIn = LocalDateTime.now().minusDays(1),
      inactiveReason = null
    )
  }
}

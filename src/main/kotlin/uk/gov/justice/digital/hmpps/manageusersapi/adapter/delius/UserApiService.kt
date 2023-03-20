package uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.config.DeliusRoleMappings
import uk.gov.justice.digital.hmpps.manageusersapi.model.DeliusUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserRole

@Service
class UserApiService(
  @Qualifier("deliusWebClientUtils") val serviceWebClientUtils: WebClientUtils,
  deliusRoleMappings: DeliusRoleMappings,

) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val mappings: Map<String, List<String>> =
    deliusRoleMappings.mappings.mapKeys { it.key.uppercase().replace('.', '_') }

  fun findUserByUsername(username: String): DeliusUser? {
    if ("@" in username) {
      log.debug("Delius not called with username as contained @: {}", username)
      return null
    }
    return serviceWebClientUtils.getIgnoreError(
      "/users/$username/details",
      DeliusUser::class.java,
    )?.let { user -> mapUserDetailsToDeliusUser(user) }
  }

  private fun mapUserDetailsToDeliusUser(userDetails: DeliusUser): DeliusUser =
    DeliusUser(
      username = userDetails.username.uppercase(),
      userId = userDetails.userId,
      firstName = userDetails.firstName,
      surname = userDetails.surname,
      email = userDetails.email.lowercase(),
      enabled = userDetails.enabled,
      roles = userDetails.roles?.let { mapUserRolesToAuthorities(it) },
    )

  fun mapUserRolesToAuthorities(userRoles: List<UserRole>) =
    userRoles.mapNotNull { (name) -> mappings[name] }
      .flatMap { r -> r.map(::UserRole) }
}

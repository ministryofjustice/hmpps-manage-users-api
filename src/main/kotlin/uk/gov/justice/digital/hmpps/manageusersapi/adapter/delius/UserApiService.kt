package uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.DeliusUserDetails

@Service
class UserApiService(
  @Qualifier("deliusWebClientUtils") val deliusWebClientUtils: WebClientUtils

) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findUserByUsername(username: String): DeliusUserDetails? {
    if ("@" in username) {
      log.debug("Delius not called with username as contained @: {}", username)
      return null
    }
    return deliusWebClientUtils.getIgnoreError(
      "/users/$username/details",
      DeliusUserDetails::class.java
    )
  }
}

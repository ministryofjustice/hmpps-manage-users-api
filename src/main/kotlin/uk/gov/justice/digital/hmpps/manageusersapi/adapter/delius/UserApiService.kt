package uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.DeliusUser

@Service
class UserApiService(
  @Qualifier("deliusWebClientUtils") val serviceWebClientUtils: WebClientUtils

) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findUserByUsername(username: String): DeliusUser? {
    if ("@" in username) {
      log.debug("Delius not called with username as contained @: {}", username)
      return null
    }
    return serviceWebClientUtils.getIgnoreError(
      "/users/$username/details",
      DeliusUser::class.java
    )
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.EmailNotificationDto
import java.util.UUID

@Service
class UserApiService(
  @Qualifier("externalUsersWebClientUtils") val externalUsersWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun enableUserById(userId: UUID): EmailNotificationDto {
    log.debug("Enabling User for User Id of {} ", userId)
    return externalUsersWebClientUtils.putWithResponse("/users/$userId/enable", EmailNotificationDto::class.java)
  }

  fun disableUserById(userId: UUID, deactivateReason: DeactivateReason) {
    log.debug("Disabling User for User Id of {} ", userId)
    externalUsersWebClientUtils.put("/users/$userId/disable", deactivateReason)
  }
}

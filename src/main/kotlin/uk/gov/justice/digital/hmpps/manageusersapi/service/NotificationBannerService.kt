package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationMessage
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationPage

@Service
class NotificationBannerService(
  @Value("\${notification.banner.roles}") private val rolesNotificationBanner: String,
  @Value("\${notification.banner.empty}") private val emptyNotificationBanner: String,
  @Value("\${notification.banner.dpsmenu}") private val dpsmenuNotificationBanner: String,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getNotificationMessage(type: NotificationPage): NotificationMessage {
    log.info("$type of notification requested")
    return when (type) {
      NotificationPage.ROLES -> {
        NotificationMessage(rolesNotificationBanner)
      }
      NotificationPage.DPSMENU -> {
        NotificationMessage(dpsmenuNotificationBanner)
      }
      NotificationPage.EMPTY -> {
        NotificationMessage(emptyNotificationBanner)
      }
    }
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationMessage
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationPage

@Service
class NotificationBannerService(
  @Value("\${notification.banner.roles}") private val rolesNotificationBanner: String,
  @Value("\${notification.banner.search}") private val searchNotificationBanner: String,
) {

  fun getNotificationMessage(type: NotificationPage): NotificationMessage {
    return when (type) {
      NotificationPage.ROLES -> {
        NotificationMessage(rolesNotificationBanner)
      }
      NotificationPage.SEARCH -> {
        NotificationMessage(searchNotificationBanner)
      }
    }
  }
}

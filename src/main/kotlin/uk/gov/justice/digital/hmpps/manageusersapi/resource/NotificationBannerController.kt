package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.service.NotificationBannerService

@RestController
class NotificationBannerController(
  private val notificationBannerService: NotificationBannerService,
) {

  @GetMapping("/notification/banner/{page}", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getNotificationBannerMessage(
    @Schema(description = "The notification page", example = "roles", required = true)
    @PathVariable
    page: NotificationPage,
  ): NotificationMessage = notificationBannerService.getNotificationMessage(page)
}

enum class NotificationPage {
  ROLES,
  EMPTY,
  DPSMENU,
}

@Schema(description = "Notification message")
data class NotificationMessage(
  @Schema(required = true, description = "Message", example = "Message string to be displayed in the notification banner")
  val message: String,
)

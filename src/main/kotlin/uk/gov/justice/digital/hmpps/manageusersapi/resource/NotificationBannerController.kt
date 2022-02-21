package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.service.NotificationBannerService

@RestController
class NotificationBannerController(
  private val notificationBannerService: NotificationBannerService,
  @Value("\${notification.banner.folder}") private val notificationBannerEnv: String,
) {

  @GetMapping("/notification/banner/{type}", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getRoleBannerMessage(
    @Schema(description = "The notification type", example = "roles", required = true)
    @PathVariable type: NotificationType,

  ): NotificationMessage = notificationBannerService.getNotificationMessage(type.filePrefix, notificationBannerEnv)
}

enum class NotificationType(val filePrefix: String) {
  ROLES("roles"),
}

@Schema(description = "Role Details")
data class NotificationMessage(
  @Schema(required = true, description = "Message", example = "Role message")
  val message: String,

) {
  constructor(n: NotificationMessage) : this(
    n.message,
  )
}

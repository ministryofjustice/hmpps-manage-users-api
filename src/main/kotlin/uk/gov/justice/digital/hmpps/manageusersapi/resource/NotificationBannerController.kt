package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.NotificationBannerService

@RestController
class NotificationBannerController(
  private val notificationBannerService: NotificationBannerService,
) {

  @GetMapping("/notification/banner/{page}", produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Gets Notification message",
    description = "Message string to be displayed in the notification banner.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "400",
        description = "BadRequest.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
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

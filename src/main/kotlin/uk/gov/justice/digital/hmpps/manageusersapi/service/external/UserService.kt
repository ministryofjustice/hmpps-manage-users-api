package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailNotificationService
import java.util.UUID

@Service("ExternalUserService")
class UserService(
  private val userApiService: UserApiService,
  private val emailNotificationService: EmailNotificationService,
  private val telemetryClient: TelemetryClient,
) {

  fun enableUserByUserId(userId: UUID) {
    val emailNotificationDto = userApiService.enableUserById(userId)
    emailNotificationDto.email?.let {
      emailNotificationService.sendEnableEmail(emailNotificationDto)
    } ?: run {
      log.warn("Notification email not sent for user {}", emailNotificationDto)
    }
    telemetryClient.trackEvent(
      "ExternalUserEnabled",
      mapOf("username" to emailNotificationDto.username, "admin" to emailNotificationDto.admin),
      null
    )
  }
  fun disableUserByUserId(userId: UUID, deactivateReason: DeactivateReason) =
    userApiService.disableUserById(userId, deactivateReason)

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class EmailNotificationDto(
  @Schema(description = "Username", example = "TEST_USER")
  val username: String,

  @Schema(description = "First name of the user", example = "John")
  val firstName: String,

  @Schema(description = "email of the user", example = "Smith@gov.uk")
  val email: String?,

  @Schema(description = "admin id who enabled user", example = "ADMIN_USR")
  val admin: String,

)

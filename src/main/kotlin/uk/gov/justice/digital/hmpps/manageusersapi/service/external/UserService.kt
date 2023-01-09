package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.utils.EmailHelper
import java.util.UUID

@Service("ExternalUserService")
class UserService(
  private val userApiService: UserApiService,
  private val emailNotificationService: EmailNotificationService,
  private val telemetryClient: TelemetryClient,
  private val externalUsersApiService: UserSearchApiService,
  private val verifyEmailService: VerifyEmailService,
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

  @Transactional
  // @Throws(ValidEmailException::class, NotificationClientException::class, AuthUserGroupRelationshipException::class, UsernameNotFoundException::class)
  fun amendUserEmailByUserId(
    userId: UUID,
    emailAddressInput: String?,
    url: String,
  ): String {
    val user = externalUsersApiService.findUserByUserId(userId)
    val username = user.username
    // TODO:3  password is currently not returned as part, below statement 'user.password'
    // if (user.password != null) {
    if (user != null) {
      return verifyEmailService.changeEmailAndRequestVerification(
        username,
        emailAddressInput,
        user.firstName,
        username, // TODO: user.name
        url.replace("initial-password", "verify-email-confirm")
      ).link
    }
    val email = EmailHelper.format(emailAddressInput)
    // verifyEmailService.validateEmailAddress(email, emailType)
    if (user.email == username.lowercase()) {
      // TODO 4. need to raise ValidEmailException if user exists
      externalUsersApiService.findUserByUsername(email!!.uppercase())
      // throw VerifyEmailService.ValidEmailException("duplicate")

      user.username = email
      telemetryClient.trackEvent(
        "ExternalUserChangeUsername",
        mapOf("username" to user.username, "previous" to username),
        null
      )
    }
    if (email != null) {
      user.email = email
    }
    user.verified = false
    val (resetLink, _) = Pair("setPasswordLink", "savedUser.id") // TODO 5. saveAndSendInitialEmail(url, user, admin, "AuthUserAmend", user.groups)
    return resetLink
  }

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

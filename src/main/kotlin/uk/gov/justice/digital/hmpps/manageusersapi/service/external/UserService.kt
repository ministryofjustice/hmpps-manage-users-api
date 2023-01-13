package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.utils.EmailHelper
import java.util.UUID
import uk.gov.service.notify.NotificationClientException

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

  // @Throws(ValidEmailException::class, NotificationClientException::class, AuthUserGroupRelationshipException::class, UsernameNotFoundException::class)
  fun amendUserEmailByUserId(
    userId: UUID,
    emailAddressInput: String?,
    url: String,
  ): String {

    val user = externalUsersApiService.findUserDetailsByUserIdForEmailUpdate(userId)
    val username = user.username
    var usernameForUpdate = user.username

    if (user.passwordPresent) {
      return verifyEmailService.changeEmailAndRequestVerification(
        username,
        emailAddressInput,
        user.firstName,
        "${user.firstName} ${user.lastName}",
        url.replace("initial-password", "verify-email-confirm")
      ).link
    }

    val email = EmailHelper.format(emailAddressInput)
    verifyEmailService.validateEmailAddress(email, EmailType.PRIMARY)

    if (user.email == username.lowercase()) {
      externalUsersApiService.findUserByUsername(email!!.uppercase()) ?: throw VerifyEmailService.ValidEmailException("duplicate")
      usernameForUpdate = email

      telemetryClient.trackEvent(
        "ExternalUserChangeUsername",
        mapOf("username" to user.username, "previous" to username),
        null
      )
    }

    val (resetLink, _) = saveAndSendInitialEmail(url, userId, email, usernameForUpdate,"AuthUserAmend")
    return resetLink
  }

  @Throws(NotificationClientException::class)
  private fun saveAndSendInitialEmail(
    url: String,
    userId: UUID,
    newEmail: String?,
    newUserName: String,
    eventPrefix: String,
  ): Pair<String, UUID?> {

    // TODO add end point to Auth (and call) here to give users more time to do the re-set - this end point will return the token for use in the password link below
    // then the reset token
    val userToken = user.createToken(UserToken.TokenType.RESET)
    // give users more time to do the reset
    userToken.tokenExpiry = LocalDateTime.now().plusDays(7)

    // TODO call new external users end point to update the email and username (in some cases the username will be unchanged)
    val savedUser = userRepository.save(user)

    // TODO return the support link from earlier external users call - this will remove the need for groups
    // support link
    val supportLink = getInitialEmailSupportLink(groups)


    val setPasswordLink = url + userToken.token
    val username = user.username
    val email = user.email
    val parameters = mapOf(
      "firstName" to user.name,
      "fullName" to user.name,
      "resetLink" to setPasswordLink,
      "supportLink" to supportLink
    )
    // send the email
    try {
      log.info("Sending initial set password to notify for user {}", username)
      notificationClient.sendEmail(initialPasswordTemplateId, email, parameters, null)
      telemetryClient.trackEvent("${eventPrefix}Success", mapOf("username" to username, "admin" to creator), null)
    } catch (e: NotificationClientException) {
      val reason = (e.cause?.let { e.cause } ?: e).javaClass.simpleName
      log.warn("Failed to send create user notify for user {}", username, e)
      telemetryClient.trackEvent(
        "${eventPrefix}Failure",
        mapOf("username" to username, "reason" to reason, "admin" to creator),
        null
      )
      if (e.httpResult >= 500) { // second time lucky
        notificationClient.sendEmail(initialPasswordTemplateId, email, parameters, null, null)
        telemetryClient.trackEvent("${eventPrefix}Success", mapOf("username" to username, "admin" to creator), null)
      }
      throw e
    }
    // return the reset link and userId to the controller
    return Pair(setPasswordLink, savedUser.id)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ExternalUserDetailsForEmailUpdateDto(
  val username: String,

  val firstName: String? = null,

  val lastName: String? = null,

  val email: String? = null,

  val passwordPresent: Boolean
)

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

package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationDetails
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserGroupApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailHelper
import java.util.UUID

@Service("ExternalUserService")
class UserService(
  private val notificationService: NotificationService,
  private val externalUsersApiService: UserApiService,
  private val externalUsersSearchApiService: UserSearchApiService,
  private val authApiService: AuthApiService,
  private val userGroupApiService: UserGroupApiService,
  private val verifyEmailService: VerifyEmailService,
  private val telemetryClient: TelemetryClient,
  @Value("\${hmpps-auth.endpoint.url}") private val authBaseUri: String,
  @Value("\${application.notify.create-initial-password.template}") private val initialPasswordTemplateId: String,
  @Value("\${application.notify.enable-user.template}") private val enableUserTemplateId: String,
) {

  fun enableUserByUserId(userId: UUID) {
    val emailNotificationDto = externalUsersApiService.enableUserById(userId)

    emailNotificationDto.email?.let {
      with(emailNotificationDto) {
        val parameters = mapOf(
          "firstName" to firstName,
          "username" to username,
          "signinUrl" to authBaseUri,
        )

        notificationService.send(enableUserTemplateId, parameters, "ExternalUserEnabledEmail", NotificationDetails(username, email!!))
      }
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
    externalUsersApiService.disableUserById(userId, deactivateReason)

  fun amendUserEmailByUserId(
    userId: UUID,
    emailAddressInput: String?
  ): String {

    val url = "$authBaseUri/initial-password?token="
    val user = externalUsersSearchApiService.findByUserId(userId)
    val username = user.username
    var usernameForUpdate = user.username

    if (externalUsersApiService.hasPassword(userId)) {

      val linkEmailAndUsername = verifyEmailService.requestVerification(
        user, emailAddressInput,
        url.replace("initial-password", "verify-email-confirm")
      )
      externalUsersApiService.updateUserEmailAddressAndUsername(userId, linkEmailAndUsername.username, linkEmailAndUsername.email)
      return linkEmailAndUsername.link
    }

    val newEmail = EmailHelper.format(emailAddressInput)
    verifyEmailService.validateEmailAddress(newEmail)

    if (user.email == username.lowercase()) {
      usernameForUpdate = verifyEmailService.confirmUsernameValidForUpdate(newEmail!!, username)
    }

    val setPasswordLink = sendInitialEmail(url, userId, user, newEmail!!, usernameForUpdate, "AuthUserAmend")
    externalUsersApiService.updateUserEmailAddressAndUsername(userId, usernameForUpdate, newEmail)
    return setPasswordLink
  }

  private fun sendInitialEmail(
    url: String,
    userId: UUID,
    user: ExternalUser,
    newEmail: String,
    newUserName: String,
    eventPrefix: String,
  ): String {

    val userToken = authApiService.createResetTokenForUser(userId)
    val supportLink = getInitialEmailSupportLink(userId)
    val setPasswordLink = url + userToken

    val name = "${user.firstName} ${user.lastName}"

    val parameters = mapOf(
      "firstName" to name,
      "fullName" to name,
      "resetLink" to setPasswordLink,
      "supportLink" to supportLink
    )

    notificationService.send(initialPasswordTemplateId, parameters, eventPrefix, NotificationDetails(newUserName, newEmail))
    return setPasswordLink
  }

  private fun getInitialEmailSupportLink(userId: UUID): String {
    val userGroups = userGroupApiService.getUserGroups(userId, false)
    val serviceCode = userGroups.firstOrNull { it.groupCode.startsWith("PECS") }?.let { "book-a-secure-move-ui" } ?: "prison-staff-hub"
    authApiService.findServiceByServiceCode(serviceCode).contact?.let {
      return it
    }

    throw RuntimeException("Failed to retrieve contact details for service code $serviceCode from Auth")
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

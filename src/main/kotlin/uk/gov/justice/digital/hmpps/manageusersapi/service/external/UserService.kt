package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserGroupApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailHelper
import uk.gov.justice.digital.hmpps.manageusersapi.service.NotificationService
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
) {

  fun enableUserByUserId(userId: UUID) {
    val enabledUser = externalUsersApiService.enableUserById(userId)
    notificationService.externalUserEnabledNotification(enabledUser)
    telemetryClient.trackEvent(
      "ExternalUserEnabled",
      mapOf("username" to enabledUser.username, "admin" to enabledUser.admin),
      null
    )
  }
  fun disableUserByUserId(userId: UUID, deactivateReason: DeactivateReason) =
    externalUsersApiService.disableUserById(userId, deactivateReason)

  fun amendUserEmailByUserId(
    userId: UUID,
    emailAddressInput: String?
  ): String {

    val user = externalUsersSearchApiService.findByUserId(userId)
    val username = user.username
    var usernameForUpdate = user.username

    if (externalUsersApiService.hasPassword(userId)) {

      val linkEmailAndUsername = verifyEmailService.requestVerification(user, emailAddressInput)
      externalUsersApiService.updateUserEmailAddressAndUsername(userId, linkEmailAndUsername.username, linkEmailAndUsername.email)
      return linkEmailAndUsername.link
    }

    val newEmail = EmailHelper.format(emailAddressInput)
    verifyEmailService.validateEmailAddress(newEmail)

    if (user.email == username.lowercase()) {
      usernameForUpdate = verifyEmailService.confirmUsernameValidForUpdate(newEmail!!, username)
    }

    val supportLink = initialNotificationSupportLink(userId)
    val setPasswordLink = notificationService.externalUserEmailAmendInitialNotification(
      userId, user, newEmail!!, usernameForUpdate, supportLink
    )
    externalUsersApiService.updateUserEmailAddressAndUsername(userId, usernameForUpdate, newEmail)
    return setPasswordLink
  }

  private fun initialNotificationSupportLink(userId: UUID): String {
    val userGroups = userGroupApiService.getUserGroups(userId, false)
    val serviceCode = userGroups.firstOrNull { it.groupCode.startsWith("PECS") }?.let { "book-a-secure-move-ui" } ?: "prison-staff-hub"
    authApiService.findServiceByServiceCode(serviceCode).contact?.let {
      return it
    }

    throw RuntimeException("Failed to retrieve contact details for service code $serviceCode from Auth")
  }
}

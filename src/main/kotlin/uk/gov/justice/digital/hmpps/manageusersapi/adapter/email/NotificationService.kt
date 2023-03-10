package uk.gov.justice.digital.hmpps.manageusersapi.adapter.email

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.CreateTokenRequest
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.TokenByEmailTypeRequest
import uk.gov.justice.digital.hmpps.manageusersapi.model.EnabledExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.EmailType
import java.util.UUID

@Service
class NotificationService(
  private val emailAdapter: EmailAdapter,
  private val authService: AuthApiService,
  @Value("\${hmpps-auth.endpoint.url}") val authBaseUri: String,
  @Value("\${application.notify.create-initial-password.template}") private val initialPasswordTemplateId: String,
  @Value("\${application.notify.enable-user.template}") private val enableUserTemplateId: String,
  @Value("\${application.notify.verify.template}") private val notifyTemplateId: String
) {

  fun newPrisonUserNotification(
    user: CreateUserRequest,
    eventPrefix: String,
  ) {
    val token = authService.createNewToken(
      CreateTokenRequest(
        username = user.username,
        email = user.email, "nomis", firstName = user.firstName, lastName = user.lastName
      )
    )
    val passwordLink = buildInitialPasswordLink(token)

    val username = user.username
    val email = user.email
    val parameters = mapOf(
      "firstName" to user.firstName,
      "fullName" to user.firstName,
      "resetLink" to passwordLink
    )

    emailAdapter.send(initialPasswordTemplateId, parameters, eventPrefix, username, email)
  }

  fun externalUserEnabledNotification(enabledUser: EnabledExternalUser) {
    enabledUser.email?.let {
      with(enabledUser) {
        val parameters = mapOf(
          "firstName" to firstName,
          "username" to username,
          "signinUrl" to authBaseUri,
        )

        emailAdapter.send(enableUserTemplateId, parameters, "ExternalUserEnabledEmail", username, email!!)
      }
    } ?: run {
      log.warn("Notification email not sent for user {}", enabledUser)
    }
  }

  fun externalUserEmailAmendInitialNotification(
    userId: UUID,
    user: ExternalUser,
    newEmail: String,
    newUserName: String,
    supportLink: String
  ): String {

    val setPasswordLink = buildInitialPasswordLink(authService.createResetTokenForUser(userId))

    val name = "${user.firstName} ${user.lastName}"

    val parameters = mapOf(
      "firstName" to name,
      "fullName" to name,
      "resetLink" to setPasswordLink,
      "supportLink" to supportLink
    )

    emailAdapter.send(initialPasswordTemplateId, parameters, "AuthUserAmend", newUserName, newEmail)
    return setPasswordLink
  }

  fun externalUserVerifyEmailNotification(userDetails: ExternalUser, email: String): String {
    val verifyLink = buildLink(
      authService.createTokenByEmailType(TokenByEmailTypeRequest(userDetails.username, EmailType.PRIMARY.name)),
      "verify-email-confirm"
    )

    val parameters: Map<String, Any> = mapOf(
      "firstName" to userDetails.firstName,
      "fullName" to "${userDetails.firstName} ${userDetails.lastName}",
      "verifyLink" to verifyLink
    )

    emailAdapter.send(notifyTemplateId, parameters, "VerifyEmailRequest", userDetails.username, email)
    return verifyLink
  }

  private fun buildInitialPasswordLink(token: String): String {
    return buildLink(token, "initial-password")
  }

  private fun buildLink(token: String, purpose: String): String {
    return "$authBaseUri/$purpose?token=$token"
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

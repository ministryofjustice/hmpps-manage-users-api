package uk.gov.justice.digital.hmpps.manageusersapi.adapter.email

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.CreateTokenRequest
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.TokenByEmailTypeRequest
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserIdentity
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.EmailType
import java.util.UUID

@Service
class NotificationService(
  private val emailAdapter: EmailAdapter,
  private val authService: AuthApiService,
  @Value("\${hmpps-auth.external.endpoint.url}") val authBaseUri: String,
  @Value("\${application.notify.create-initial-password.template}") private val initialPasswordTemplateId: String,
  @Value("\${application.notify.enable-user.template}") private val enableUserTemplateId: String,
  @Value("\${application.notify.verify.template}") private val notifyTemplateId: String,
) {

  fun newPrisonUserNotification(
    user: CreateUserRequest,
    eventPrefix: String,
  ) {
    val token = authService.createNewToken(
      CreateTokenRequest(
        username = user.username,
        email = user.email,
        "nomis",
        firstName = user.firstName,
        lastName = user.lastName,
      ),
    )
    val passwordLink = buildInitialPasswordLink(token)

    val username = user.username
    val email = user.email
    val parameters = mapOf(
      "firstName" to user.firstName,
      "fullName" to user.firstName,
      "resetLink" to passwordLink,
    )

    emailAdapter.send(initialPasswordTemplateId, parameters, eventPrefix, username, email)
  }

  fun externalUserEnabledNotification(enabledUser: ExternalUser) {
    with(enabledUser) {
      val parameters = mapOf(
        "firstName" to firstName,
        "username" to username,
        "signinUrl" to authBaseUri,
      )

      emailAdapter.send(enableUserTemplateId, parameters, "ExternalUserEnabledEmail", username, email)
    }
  }

  fun externalUserInitialNotification(
    userId: UUID,
    firstName: String,
    lastName: String,
    username: String,
    emailAddress: String,
    supportLink: String,
    eventPrefix: String,
  ): String {
    val setPasswordLink = buildInitialPasswordLink(authService.createResetTokenForUser(userId))
    val name = "$firstName $lastName"
    val parameters = mapOf(
      "firstName" to name,
      "fullName" to name,
      "resetLink" to setPasswordLink,
      "supportLink" to supportLink,
    )

    emailAdapter.send(initialPasswordTemplateId, parameters, eventPrefix, username, emailAddress)
    return setPasswordLink
  }

  fun verifyEmailNotification(userDetails: UserIdentity, email: String): String {
    val verifyLink = buildLink(
      authService.createTokenByEmailType(TokenByEmailTypeRequest(userDetails.username, EmailType.PRIMARY.name)),
      "verify-email-confirm",
    )

    val parameters: Map<String, Any> = mapOf(
      "firstName" to userDetails.firstName,
      "fullName" to "${userDetails.firstName} ${userDetails.lastName}",
      "verifyLink" to verifyLink,
    )

    emailAdapter.send(notifyTemplateId, parameters, "VerifyEmailRequest", userDetails.username, email)
    return verifyLink
  }

  private fun buildInitialPasswordLink(token: String): String = buildLink(token, "initial-password")

  private fun buildLink(token: String, purpose: String): String = "$authBaseUri/$purpose?token=$token"
}

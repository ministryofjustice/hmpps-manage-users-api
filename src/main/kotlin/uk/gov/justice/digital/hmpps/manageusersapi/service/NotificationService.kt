package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.CreateTokenRequest
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.EmailNotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest

@Service
class NotificationService(
  private val notificationService: EmailNotificationService,
  private val authService: AuthApiService,
  @Value("\${hmpps-auth.endpoint.url}") val authBaseUri: String,
  @Value("\${application.notify.create-initial-password.template}") private val initialPasswordTemplateId: String,
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
    val passwordLink = getPasswordResetLink(token)

    val username = user.username
    val email = user.email
    val parameters = mapOf(
      "firstName" to user.firstName,
      "fullName" to user.firstName,
      "resetLink" to passwordLink
    )

    notificationService.send(initialPasswordTemplateId, parameters, eventPrefix, username, email)
  }

  private fun getPasswordResetLink(token: String): String {
    return "$authBaseUri/initial-password?token=$token"
  }
}

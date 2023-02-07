package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.TokenByEmailTypeRequest
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationDetails
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailHelper

@Service
@Transactional(readOnly = true)
class VerifyEmailService(
  private val telemetryClient: TelemetryClient,
  private val notificationService: NotificationService,
  private val verifyEmailDomainService: VerifyEmailDomainService,
  private val externalUserSearchApiService: UserSearchApiService,
  private val externalUsersApiService: UserApiService,
  private val authApiService: AuthApiService,
  @Value("\${application.notify.verify.template}") private val notifyTemplateId: String
) {

  fun changeEmailAndRequestVerification(
    userDetails: ExternalUserDetailsDto,
    emailInput: String?,
    url: String,
  ): LinkEmailAndUsername {

    val verifyLink =
      url + authApiService.createTokenByEmailType(TokenByEmailTypeRequest(userDetails.username, EmailType.PRIMARY.name))

    val email = EmailHelper.format(emailInput)
    var usernameToUpdate = userDetails.username
    validateEmailAddress(email, EmailType.PRIMARY)

    if (userDetails.username.contains("@") && email!!.uppercase() != userDetails.username) {
      usernameToUpdate = confirmUsernameForUpdate(email, userDetails.username)
    }

    val parameters: Map<String, Any> = mapOf(
      "firstName" to userDetails.firstName,
      "fullName" to "${userDetails.firstName} ${userDetails.lastName}",
      "verifyLink" to verifyLink
    )

    notificationService.send(notifyTemplateId, parameters, "VerifyEmailRequest", NotificationDetails(userDetails.username, email!!))
    externalUsersApiService.updateUserEmailAddressAndUsername(userDetails.userId, usernameToUpdate, email)
    return LinkEmailAndUsername(verifyLink, email, usernameToUpdate)
  }

  fun confirmUsernameForUpdate(newEmail: String, existingUsername: String): String {
    externalUserSearchApiService.findUserByUsernameIfPresent(newEmail.uppercase())?.let {
      throw ValidEmailException("duplicate")
    }

    telemetryClient.trackEvent(
      "ExternalUserChangeUsername",
      mapOf("username" to newEmail, "previous" to existingUsername),
      null
    )

    return newEmail
  }

  fun validateEmailAddress(email: String?, emailType: EmailType): Boolean {
    if (email.isNullOrBlank()) {
      throw ValidEmailException("blank")
    }
    if (email.length > MAX_LENGTH_EMAIL) throw ValidEmailException("maxlength")
    validateEmailAddressExcludingGsi(email, emailType)
    if (email.matches(Regex(".*@.*\\.gsi\\.gov\\.uk"))) throw ValidEmailException("gsi")
    return true
  }

  fun validateEmailAddressExcludingGsi(email: String, emailType: EmailType) {
    val atIndex = email.indexOf('@')
    if (atIndex == -1 || !email.matches(Regex(".*@.*\\..*"))) {
      throw ValidEmailException("format")
    }
    val firstCharacter = email[0]
    val lastCharacter = email[email.length - 1]
    if (firstCharacter == '.' || firstCharacter == '@' || lastCharacter == '.' || lastCharacter == '@') {
      throw ValidEmailException("firstlast")
    }
    if (email.matches(Regex(".*\\.@.*")) || email.matches(Regex(".*@\\..*"))) {
      throw ValidEmailException("together")
    }
    if (StringUtils.countMatches(email, '@') > 1) {
      throw ValidEmailException("at")
    }
    if (StringUtils.containsWhitespace(email)) {
      throw ValidEmailException("white")
    }
    if (!email.matches(Regex("[0-9A-Za-z@.'_\\-+]*"))) {
      throw ValidEmailException("characters")
    }
    if (emailType == EmailType.PRIMARY && !verifyEmailDomainService.isValidEmailDomain(email.substring(atIndex + 1))) {
      throw ValidEmailException("domain")
    }
  }

  class ValidEmailException(val reason: String) : Exception("Validate email failed with reason: $reason")

  data class LinkEmailAndUsername(val link: String, val email: String, val username: String)

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private const val MAX_LENGTH_EMAIL = 240
  }
}

enum class EmailType(val description: String) {
  PRIMARY("primary"),
  SECONDARY("secondary");
}

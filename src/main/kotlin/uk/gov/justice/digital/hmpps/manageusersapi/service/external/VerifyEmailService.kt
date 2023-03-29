package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserIdentity
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailHelper

@Service
@Transactional(readOnly = true)
class VerifyEmailService(
  private val telemetryClient: TelemetryClient,
  private val notificationService: NotificationService,
  private val verifyEmailDomainService: VerifyEmailDomainService,
  private val externalUserSearchApiService: UserSearchApiService,
) {

  fun requestVerification(
    userDetails: UserIdentity,
    emailInput: String?,
  ): LinkEmailAndUsername {
    val email = EmailHelper.format(emailInput)
    var usernameToUpdate = userDetails.username
    validateEmailAddress(email)

    if (userDetails.username.contains("@") && email!!.uppercase() != userDetails.username) {
      usernameToUpdate = confirmUsernameValidForUpdate(email, userDetails.username)
    }

    val verifyLink = notificationService.verifyEmailNotification(userDetails, email!!)
    return LinkEmailAndUsername(verifyLink, email, usernameToUpdate)
  }

  fun confirmUsernameValidForUpdate(newEmail: String, existingUsername: String): String {
    externalUserSearchApiService.findUserByUsernameIfPresent(newEmail.uppercase())?.let {
      throw ValidEmailException("duplicate")
    }

    telemetryClient.trackEvent(
      "ExternalUserChangeUsername",
      mapOf("username" to newEmail, "previous" to existingUsername),
      null,
    )

    return newEmail
  }

  fun validateEmailAddress(email: String?): Boolean {
    if (email.isNullOrBlank()) {
      throw ValidEmailException("blank")
    }
    if (email.length > MAX_LENGTH_EMAIL) throw ValidEmailException("maxlength")
    validateEmailAddressExcludingGsi(email)
    if (email.matches(Regex(".*@.*\\.gsi\\.gov\\.uk"))) throw ValidEmailException("gsi")
    return true
  }

  fun validateEmailAddressExcludingGsi(email: String) {
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
    if (!verifyEmailDomainService.isValidEmailDomain(email.substring(atIndex + 1))) {
      throw ValidEmailException("domain")
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private const val MAX_LENGTH_EMAIL = 240
  }
}

class ValidEmailException(val reason: String) : RuntimeException("Validate email failed with reason: $reason")

data class LinkEmailAndUsername(val link: String, val email: String, val username: String)

enum class EmailType(val description: String) {
  PRIMARY("primary"),
}

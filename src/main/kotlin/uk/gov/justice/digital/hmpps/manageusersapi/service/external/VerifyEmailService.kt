package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.AuthService
import uk.gov.justice.digital.hmpps.manageusersapi.service.TokenByEmailTypeRequest
import uk.gov.justice.digital.hmpps.manageusersapi.utils.EmailHelper
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException

@Service
@Transactional(readOnly = true)
class VerifyEmailService(
  // private val userRepository: UserRepository,
  // private val userTokenRepository: UserTokenRepository,
  private val telemetryClient: TelemetryClient,
  private val notificationClient: NotificationClientApi,
  private val verifyEmailDomainService: VerifyEmailDomainService,
  private val externalUsersApiService: UserSearchApiService,
  private val authService: AuthService,
  // private val nomisUserApiService: NomisUserApiService,
  // @Value("\${application.notify.verify.template}") private val notifyTemplateId: String,

) {

  // TODO wire this in from config
  private val notifyTemplateId: String = "template"

  @Transactional
  @Throws(NotificationClientException::class, ValidEmailException::class)
  fun changeEmailAndRequestVerification(
    username: String,
    emailInput: String?,
    firstName: String?,
    fullName: String?,
    url: String,
  ): LinkEmailAndUsername {
    // TODO Confirm in Auth but this call should not be necessary as in our context we already have the user
    // val user = externalUsersApiService.findUserByUserId(userId)

    val verifyLink =
      url + authService.createTokenByEmailType(TokenByEmailTypeRequest(username, EmailType.PRIMARY.name))

    val parameters = mapOf(
      "firstName" to firstName,
      "fullName" to fullName,
      "verifyLink" to verifyLink)

    val email = EmailHelper.format(emailInput)
    var usernameToUpdate = username
    validateEmailAddress(email, EmailType.PRIMARY)

    username.let {

      // if the user is configured so that the email address is their username, need to check it is unique
      if (it.contains("@") && email!!.uppercase() != username) {
        val userWithEmailInDatabase = externalUsersApiService.findUserByUsername(email.uppercase())
        if(userWithEmailInDatabase != null) {
          throw ValidEmailException("duplicate")
        } else {
          usernameToUpdate = email
          telemetryClient.trackEvent(
            "ExternalUserChangeUsername",
            mapOf("username" to usernameToUpdate, "previous" to username),
            null
          )
        }
      }
    }

    try {
      log.info("Sending email verification to notify for user {}", username)
      notificationClient.sendEmail(notifyTemplateId, email, parameters, null)
      telemetryClient.trackEvent("VerifyEmailRequestSuccess", mapOf("username" to username), null)
    } catch (e: NotificationClientException) {
      val reason = (if (e.cause != null) e.cause else e)?.javaClass?.simpleName
      log.warn("Failed to send email verification to notify for user {}", username, e)
      telemetryClient.trackEvent(
        "VerifyEmailRequestFailure",
        mapOf("username" to username, "reason" to reason),
        null
      )
      if (e.httpResult >= 500) {
        // second time lucky
        notificationClient.sendEmail(notifyTemplateId, email, parameters, null, null)
      }
      throw e
    }

    // TODO call new external users end point to update the email and username, (in some cases the username will be unchanged). Internally this will set verified to false
    // userRepository.save(user)

    return LinkEmailAndUsername(verifyLink, email!!, username)
  }

  @Throws(ValidEmailException::class)
  fun validateEmailAddress(email: String?, emailType: EmailType): Boolean {
    if (email.isNullOrBlank()) {
      throw ValidEmailException("blank")
    }
    if (email.length > MAX_LENGTH_EMAIL) throw ValidEmailException("maxlength")
    validateEmailAddressExcludingGsi(email, emailType)
    if (email.matches(Regex(".*@.*\\.gsi\\.gov\\.uk"))) throw ValidEmailException("gsi")
    return true
  }

  @Throws(ValidEmailException::class)
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

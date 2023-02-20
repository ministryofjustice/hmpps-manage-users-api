package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.TokenByEmailTypeRequest
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationDetails
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture
import java.util.UUID

class VerifyEmailServiceTest {

  private val telemetryClient: TelemetryClient = mock()
  private val notificationService: NotificationService = mock()
  private val verifyEmailDomainService: VerifyEmailDomainService = mock()
  private val externalUserSearchApiService: UserSearchApiService = mock()
  private val authApiService: AuthApiService = mock()

  private val userId: UUID = UUID.randomUUID()
  private val notifyTemplateId = "templateId"
  private val userFixture = UserFixture()

  private val verifyEmailService = VerifyEmailService(
    telemetryClient,
    notificationService,
    verifyEmailDomainService,
    externalUserSearchApiService,
    authApiService,
    notifyTemplateId
  )

  @Nested
  inner class RequestVerification {
    private val newEmailAddress = "new.testy@testing.com"
    private val url = "initial-password-url.digital.gov.uk"
    private val token = "a25adf13-dbed-4a19-ad07-d1cd95b12500"

    @Test
    fun shouldSendVerifyEmailAddressEmail() {
      val user = userFixture.createExternalUserDetails(userId = userId, username = "someuser", email = "testy@testing.com")
      whenever(authApiService.createTokenByEmailType(TokenByEmailTypeRequest(user.username, EmailType.PRIMARY.name))).thenReturn(token)
      whenever(verifyEmailDomainService.isValidEmailDomain("testing.com")).thenReturn(true)

      verifyEmailService.requestVerification(user, newEmailAddress, url)

      verify(notificationService).send(
        eq(notifyTemplateId),
        check {
          assertThat(it["firstName"]).isEqualTo("first")
          assertThat(it["fullName"]).isEqualTo("first last")
          assertThat(it["verifyLink"]).isEqualTo(url + token)
        },
        eq("VerifyEmailRequest"),
        eq(NotificationDetails("someuser", newEmailAddress))
      )
    }

    @Test
    fun shouldRespondWithVerifyLinkUsernameAndEmail() {
      val user = userFixture.createExternalUserDetails(userId = userId, username = "someuser", email = "testy@testing.com")
      whenever(authApiService.createTokenByEmailType(TokenByEmailTypeRequest(user.username, EmailType.PRIMARY.name))).thenReturn(token)
      whenever(verifyEmailDomainService.isValidEmailDomain("testing.com")).thenReturn(true)

      val linkEmailAndUsername = verifyEmailService.requestVerification(user, newEmailAddress, url)

      assertThat(linkEmailAndUsername.link).isEqualTo(url + token)
      assertThat(linkEmailAndUsername.email).isEqualTo(newEmailAddress)
      assertThat(linkEmailAndUsername.username).isEqualTo("someuser")
    }

    @Test
    fun shouldExitWithExceptionWhenEmailAddressAlreadyInUse() {
      val user = userFixture.createExternalUserDetails(userId = userId, username = "someuser@user.com", email = "testy@testing.com")
      whenever(authApiService.createTokenByEmailType(TokenByEmailTypeRequest(user.username, EmailType.PRIMARY.name))).thenReturn(token)
      whenever(verifyEmailDomainService.isValidEmailDomain("testing.com")).thenReturn(true)
      whenever(externalUserSearchApiService.findUserByUsernameIfPresent(anyString())).thenReturn(user)

      assertThatThrownBy { verifyEmailService.requestVerification(user, newEmailAddress, url) }.isInstanceOf(
        VerifyEmailService.ValidEmailException::class.java
      ).extracting("reason").isEqualTo("duplicate")
    }

    @Test
    fun shouldRecordUsernameChange() {
      val user = userFixture.createExternalUserDetails(userId = userId, username = "someuser@user.com", email = "testy@testing.com")
      whenever(authApiService.createTokenByEmailType(TokenByEmailTypeRequest(user.username, EmailType.PRIMARY.name))).thenReturn(token)
      whenever(verifyEmailDomainService.isValidEmailDomain("testing.com")).thenReturn(true)

      verifyEmailService.requestVerification(user, newEmailAddress, url)

      verify(telemetryClient).trackEvent(
        eq("ExternalUserChangeUsername"),
        check {
          assertThat(it["username"]).isEqualTo(newEmailAddress)
          assertThat(it["previous"]).isEqualTo(user.username)
        },
        isNull()
      )
    }

    @Test
    fun shouldRespondWithUsernameSameAsNewEmailWhenExistingUsernameIsEmailAddress() {
      val user = userFixture.createExternalUserDetails(userId = userId, username = "someuser@user.com", email = "testy@testing.com")
      whenever(authApiService.createTokenByEmailType(TokenByEmailTypeRequest(user.username, EmailType.PRIMARY.name))).thenReturn(token)
      whenever(verifyEmailDomainService.isValidEmailDomain("testing.com")).thenReturn(true)

      val linkEmailAndUsername = verifyEmailService.requestVerification(user, newEmailAddress, url)

      assertThat(linkEmailAndUsername.email).isEqualTo(newEmailAddress)
      assertThat(linkEmailAndUsername.username).isEqualTo(newEmailAddress)
    }

    @Test
    fun shouldFormatEmailInput() {
      val user = userFixture.createExternalUserDetails(userId = userId, username = "someuser@user.com", email = "testy@testing.com")
      whenever(authApiService.createTokenByEmailType(TokenByEmailTypeRequest(user.username, EmailType.PRIMARY.name))).thenReturn(token)
      whenever(verifyEmailDomainService.isValidEmailDomain("somewhere.com")).thenReturn(true)

      val linkEmailAndUsername = verifyEmailService.requestVerification(user, "    some.u’ser@SOMEwhere.COM", url)

      assertThat(linkEmailAndUsername.email).isEqualTo("some.u'ser@somewhere.com")
    }
  }

  @Nested
  inner class ValidateEmailAddress {

    @Test
    fun gsiEmail() {
      whenever(verifyEmailDomainService.isValidEmailDomain(anyString())).thenReturn(true)
      verifyPrimaryEmailFailure("some.u'ser@SOMEwhe.gsi.gov.uk", "gsi")
    }

    @Test
    fun noAtSign() {
      verifyPrimaryEmailFailure("a", "format")
    }

    @Test
    fun multipleAtSigns() {
      verifyPrimaryEmailFailure("a@b.fred@joe.com", "at")
    }

    @Test
    fun noExtension() {
      verifyPrimaryEmailFailure("a@bee", "format")
    }

    @Test
    fun firstLastStopFirst() {
      verifyPrimaryEmailFailure(".a@bee.com", "firstlast")
    }

    @Test
    fun firstLastStopLast() {
      verifyPrimaryEmailFailure("a@bee.com.", "firstlast")
    }

    @Test
    fun firstLastAtFirst() {
      verifyPrimaryEmailFailure("@a@bee.com", "firstlast")
    }

    @Test
    fun firstLastAtLast() {
      verifyPrimaryEmailFailure("a@bee.com@", "firstlast")
    }

    @Test
    fun togetherAtBefore() {
      verifyPrimaryEmailFailure("a@.com", "together")
    }

    @Test
    fun togetherAtAfter() {
      verifyPrimaryEmailFailure("a.@joe.com", "together")
    }

    @Test
    fun white() {
      verifyPrimaryEmailFailure("a@be\te.com", "white")
    }

    @Test
    fun invalidCharacters() {
      verifyPrimaryEmailFailure("a@b.&com", "characters")
    }

    @Test
    fun invalidDomain() {
      verifyPrimaryEmailFailure("a@b.com", "domain")
      verify(verifyEmailDomainService).isValidEmailDomain("b.com")
    }

    @Test
    fun `validate email exceeds email max length`() {
      val email: String = "A".repeat(241)
      assertThatThrownBy {
        verifyEmailService.validateEmailAddress(
          email
        )
      }.isInstanceOf(VerifyEmailService.ValidEmailException::class.java)
        .hasMessage("Validate email failed with reason: maxlength")
    }

    private fun verifyPrimaryEmailFailure(email: String, reason: String) {
      assertThatThrownBy { verifyEmailService.validateEmailAddress(email) }.isInstanceOf(
        VerifyEmailService.ValidEmailException::class.java
      ).extracting("reason").isEqualTo(reason)
    }
  }
}

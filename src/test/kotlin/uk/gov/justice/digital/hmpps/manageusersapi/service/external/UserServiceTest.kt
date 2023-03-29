package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserGroupApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createExternalUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthService
import uk.gov.justice.digital.hmpps.manageusersapi.model.EnabledExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import java.util.UUID

class UserServiceTest {
  private val notificationService: NotificationService = mock()
  private val userApiService: UserApiService = mock()
  private val externalUsersSearchApiService: UserSearchApiService = mock()
  private val authApiService: AuthApiService = mock()
  private val userGroupApiService: UserGroupApiService = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val userService = UserService(
    notificationService,
    userApiService,
    externalUsersSearchApiService,
    authApiService,
    userGroupApiService,
    verifyEmailService,
    telemetryClient,
  )
  private val userUUID: UUID = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")

  @Nested
  inner class EnableExternalUser {

    @Test
    fun `enable user by userId sends email`() {
      val enabledUser = EnabledExternalUser("CEN_ADM", "firstName", "cadmin@gov.uk", "admin")
      whenever(userApiService.enableUserById(anyOrNull())).thenReturn(enabledUser)

      userService.enableUserByUserId(userUUID)

      verify(notificationService).externalUserEnabledNotification(enabledUser)
    }
  }

  @Nested
  inner class DisableExternalUser {

    @Test
    fun `disable user by userId sends email`() {
      val reason = DeactivateReason("Fired")
      userService.disableUserByUserId(userUUID, reason)
      verify(userApiService).disableUserById(UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a"), reason)
    }
  }

  @Nested
  inner class AmendUserEmailByUserId {

    private val userId: UUID = UUID.randomUUID()
    private val newEmailAddress = "new.testy@testing.com"
    private val token = "a25adf13-dbed-4a19-ad07-d1cd95b12500"

    @Test
    fun `user with password - invalid email`() {
      val externalUser = createExternalUserDetails(userId, "testing", "testy@testing.com")

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(true)
      doThrow(ValidEmailException("reason"))
        .whenever(verifyEmailService).requestVerification(eq(externalUser), eq(newEmailAddress))

      assertThatThrownBy {
        userService.amendUserEmailByUserId(
          userId,
          newEmailAddress,
        )
      }.isInstanceOf(ValidEmailException::class.java).hasMessage("Validate email failed with reason: reason")

      verify(userApiService, never()).updateUserEmailAddressAndUsername(any(), anyString(), anyString())
    }

    @Test
    fun `error response on attempt to retrieve user by id`() {
      doThrow(WebClientResponseException::class).whenever(externalUsersSearchApiService).findByUserId(userId)

      assertThatThrownBy {
        userService.amendUserEmailByUserId(
          userId,
          newEmailAddress,
        )
      }.isInstanceOf(WebClientResponseException::class.java)
    }

    @Test
    fun `user with password - success link returned for`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(true)
      whenever(verifyEmailService.requestVerification(eq(externalUser), eq(newEmailAddress))).thenReturn(
        LinkEmailAndUsername("link", newEmailAddress, "testing"),
      )

      val link = userService.amendUserEmailByUserId(userId, newEmailAddress)

      assertThat(link).isEqualTo("link")
    }

    @Test
    fun `user with password - email and username updated for`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(true)
      whenever(verifyEmailService.requestVerification(eq(externalUser), eq(newEmailAddress))).thenReturn(
        LinkEmailAndUsername("link", newEmailAddress, "testing"),
      )

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(userApiService).updateUserEmailAddressAndUsername(userId, "testing", newEmailAddress)
    }

    @Test
    fun `user without password - format email input`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      whenever(authApiService.createResetTokenForUser(userId)).thenReturn(token)
      whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(listOf())
      whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(createAuthServiceWith("prison-staff-hub", "service-not-pecs@testing.com"))

      userService.amendUserEmailByUserId(userId, "    SARAH.o’connor@gov.uk")

      verify(userApiService).updateUserEmailAddressAndUsername(userId, "testing", "sarah.o'connor@gov.uk")
    }

    @Test
    fun `user without password - invalid email`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      doThrow(ValidEmailException("format")).whenever(verifyEmailService).validateEmailAddress("inv@lid@gov.uk")

      assertThatThrownBy {
        userService.amendUserEmailByUserId(
          userId,
          "inv@lid@gov.uk",
        )
      }.isInstanceOf(ValidEmailException::class.java).hasMessage("Validate email failed with reason: format")
    }

    @Test
    fun `user without password - pecs user group support link`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      whenever(authApiService.createResetTokenForUser(userId)).thenReturn(token)
      whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(listOf(UserGroup("PECS Groups", "PECS Test Group")))
      whenever(authApiService.findServiceByServiceCode("book-a-secure-move-ui")).thenReturn(createAuthServiceWith("service-pecs@testing.com", "book-a-secure-move-ui"))

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(notificationService).externalUserEmailAmendInitialNotification(
        any(),
        any(),
        anyString(),
        anyString(),
        eq("service-pecs@testing.com"),
      )
    }

    @Test
    fun `user without password - non pecs user group support link`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      whenever(authApiService.createResetTokenForUser(userId)).thenReturn(token)
      whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(listOf(UserGroup("NOT PECS Groups", "NOT PECS GROUP Test")))
      whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(createAuthServiceWith("service-not-pecs@testing.com", "prison-staff-hub"))

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(notificationService).externalUserEmailAmendInitialNotification(
        any(),
        any(),
        anyString(),
        anyString(),
        eq("service-not-pecs@testing.com"),
      )
    }

    @Test
    fun `user without password - multiple groups one pecs group support link`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      whenever(authApiService.createResetTokenForUser(userId)).thenReturn(token)
      whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(
        listOf(
          UserGroup("NOT PECS Group 1", "NOT PECS GROUP 1 Test"),
          UserGroup("NOT PECS Group 2", "NOT PECS GROUP 2 Test"),
          UserGroup("PECS Groups", "PECS Test Group"),
        ),
      )
      whenever(authApiService.findServiceByServiceCode("book-a-secure-move-ui")).thenReturn(createAuthServiceWith("service-pecs@testing.com", "book-a-secure-move-ui"))

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(notificationService).externalUserEmailAmendInitialNotification(
        any(),
        any(),
        anyString(),
        anyString(),
        eq("service-pecs@testing.com"),
      )
    }

    @Test
    fun `user without password - no groups support link`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      whenever(authApiService.createResetTokenForUser(userId)).thenReturn(token)
      whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(listOf())
      whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(createAuthServiceWith("service-not-pecs@testing.com", "prison-staff-hub"))

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(notificationService).externalUserEmailAmendInitialNotification(
        any(),
        any(),
        anyString(),
        anyString(),
        eq("service-not-pecs@testing.com"),
      )
    }

    @Test
    fun `user without password - sends initial email`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      whenever(authApiService.createResetTokenForUser(userId)).thenReturn(token)
      whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(listOf())
      whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(createAuthServiceWith("service-not-pecs@testing.com", "prison-staff-hub"))

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(notificationService).externalUserEmailAmendInitialNotification(userId, externalUser, newEmailAddress, externalUser.username, "service-not-pecs@testing.com")
    }

    @Test
    fun `user without password - changes email address if same as username`() {
      val externalUser = createExternalUserDetails(userId, "TESTY@TESTING.COM")

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      whenever(authApiService.createResetTokenForUser(userId)).thenReturn(token)
      whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(listOf())
      whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(createAuthServiceWith("service-not-pecs@testing.com", "prison-staff-hub"))
      whenever(verifyEmailService.confirmUsernameValidForUpdate(newEmailAddress, "TESTY@TESTING.COM")).thenReturn(newEmailAddress)

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(userApiService).updateUserEmailAddressAndUsername(userId, newEmailAddress, newEmailAddress)
    }

    @Test
    fun `user without password - cannot change email to same as existing user`() {
      val externalUser = createExternalUserDetails(userId, "TESTY@TESTING.COM")

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      doThrow(ValidEmailException("duplicate")).whenever(verifyEmailService).confirmUsernameValidForUpdate(newEmailAddress, "TESTY@TESTING.COM")

      assertThatThrownBy {
        userService.amendUserEmailByUserId(userId, newEmailAddress)
      }.hasMessage("Validate email failed with reason: duplicate")
    }

    private fun createAuthServiceWith(contact: String, code: String): AuthService {
      return AuthService(code, "name", "desc", contact, "url")
    }
  }
}

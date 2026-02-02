package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserGroupApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserSearchApiService
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createExternalUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthService
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.NewUser
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.service.notify.NotificationClientException
import java.util.UUID

class UserServiceTest {
  private val notificationService: NotificationService = mock()
  private val userApiService: UserApiService = mock()
  private val externalUsersSearchApiService: UserSearchApiService = mock()
  private val authApiService: AuthApiService = mock()
  private val userGroupApiService: UserGroupApiService = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val hmppsAuthenticationHolder: HmppsAuthenticationHolder = mock()

  private val syncUserUpdates = false

  private lateinit var userService: UserService

  private val userUUID: UUID = UUID.fromString("00000000-aaaa-0000-aaaa-0a0a0a0a0a0a")

  @BeforeEach
  fun setUp() {
    userService = UserService(
      notificationService,
      userApiService,
      externalUsersSearchApiService,
      authApiService,
      userGroupApiService,
      verifyEmailService,
      telemetryClient,
      hmppsAuthenticationHolder,
      syncUserUpdates,
    )
  }

  @Nested
  inner class CreateUser {
    private val emailAddress = "testy.mctester@testing.com"

    @Test
    fun `should fail when email invalid`() {
      doThrow(ValidEmailException("reason"))
        .whenever(verifyEmailService).validateEmailAddress(emailAddress)

      assertThatThrownBy {
        userService.createUser(NewUser(emailAddress, "Testy", "McTester", setOf("SITE_1_GROUP_1")))
      }.isInstanceOf(EmailException::class.java).hasMessage("email:Email address failed validation")

      verifyNoInteractions(userApiService, notificationService)
    }

    @Test
    fun `should fail when remote call to external users fails`() {
      val newUser = NewUser(emailAddress, "Testy", "McTester", setOf("SITE_1_GROUP_1"))
      doThrow(WebClientResponseException::class).whenever(
        userApiService,
      ).createUser(newUser.firstName, newUser.lastName, newUser.email, newUser.groupCodes)

      assertThatThrownBy {
        userService.createUser(newUser)
      }.isInstanceOf(WebClientResponseException::class.java)

      verifyNoInteractions(notificationService)
    }

    @Test
    fun `should fail when attempt to send email fails`() {
      val newUser = NewUser(emailAddress, "Testy", "McTester", setOf("SITE_1_GROUP_1"))
      val uuid = UUID.randomUUID()
      with(newUser) {
        whenever(userApiService.createUser(firstName, lastName, email, groupCodes)).thenReturn(uuid)
      }
      whenever(userGroupApiService.getUserGroups(uuid, false)).thenReturn(listOf())
      whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(createAuthServiceWith("prison-staff-hub", "service-not-pecs@testing.com"))

      whenever(notificationService.externalUserInitialNotification(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).then {
        throw NotificationClientException("email service failed")
      }

      assertThatThrownBy {
        userService.createUser(newUser)
      }.isInstanceOf(NotificationClientException::class.java)
    }

    @Test
    fun `should format email`() {
      val newUser = NewUser("SARAH.o’connor@gov.uk", "Testy", "McTester", setOf("SITE_1_GROUP_1"))
      val uuid = UUID.randomUUID()
      with(newUser) {
        whenever(userApiService.createUser(firstName, lastName, "sarah.o'connor@gov.uk", groupCodes)).thenReturn(uuid)

        whenever(userGroupApiService.getUserGroups(uuid, false)).thenReturn(listOf())
        whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(
          createAuthServiceWith(
            "prison-staff-hub",
            "service-not-pecs@testing.com",
          ),
        )

        userService.createUser(newUser)

        verify(notificationService).externalUserInitialNotification(uuid, firstName, lastName, "SARAH.O'CONNOR@GOV.UK", "sarah.o'connor@gov.uk", "prison-staff-hub", "ExternalUserCreate")
      }
    }

    @Test
    fun `should generate pecs user group support link`() {
      val newUser = NewUser(emailAddress, "Testy", "McTester", setOf("SITE_1_GROUP_1"))
      val uuid = UUID.randomUUID()
      with(newUser) {
        whenever(userApiService.createUser(firstName, lastName, email, groupCodes)).thenReturn(uuid)

        whenever(userGroupApiService.getUserGroups(uuid, false)).thenReturn(listOf(UserGroup("PECS Groups", "PECS Test Group")))
        whenever(authApiService.findServiceByServiceCode("book-a-secure-move-ui")).thenReturn(
          createAuthServiceWith(
            "book-a-secure-move-ui",
            "service-pecs@testing.com",
          ),
        )

        userService.createUser(newUser)

        verify(notificationService).externalUserInitialNotification(uuid, firstName, lastName, emailAddress.uppercase(), emailAddress, "book-a-secure-move-ui", "ExternalUserCreate")
      }
    }

    @Test
    fun `should generate non pecs user group support link`() {
      val newUser = NewUser(emailAddress, "Testy", "McTester", setOf("SITE_1_GROUP_1"))
      val uuid = UUID.randomUUID()
      val userId = uuid
      with(newUser) {
        whenever(userApiService.createUser(firstName, lastName, email, groupCodes)).thenReturn(userId)

        whenever(userGroupApiService.getUserGroups(uuid, false)).thenReturn(listOf(UserGroup("NON PECS Groups", "NON PECS Test Group")))
        whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(
          createAuthServiceWith(
            "prison-staff-hub",
            "service-non-pecs@testing.com",
          ),
        )

        userService.createUser(newUser)

        verify(notificationService).externalUserInitialNotification(uuid, firstName, lastName, emailAddress.uppercase(), emailAddress, "prison-staff-hub", "ExternalUserCreate")
      }
    }

    @Test
    fun `should return user id`() {
      val newUser = NewUser(emailAddress, "Testy", "McTester", setOf("SITE_1_GROUP_1"))
      val uuid = UUID.randomUUID()
      with(newUser) {
        whenever(userApiService.createUser(firstName, lastName, email, groupCodes)).thenReturn(uuid)

        whenever(userGroupApiService.getUserGroups(uuid, false)).thenReturn(listOf(UserGroup("NON PECS Groups", "NON PECS Test Group")))
        whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(
          createAuthServiceWith(
            "prison-staff-hub",
            "service-non-pecs@testing.com",
          ),
        )

        val actualUserId = userService.createUser(newUser)

        assertEquals(uuid, actualUserId)
      }
    }

    @Test
    fun `should sync user creation with Auth when user update sync enabled`() {
      givenAuthUserSyncEnabled()
      val newUser = NewUser(emailAddress, "Testy", "McTester", setOf("SITE_1_GROUP_1"))
      val uuid = UUID.randomUUID()
      with(newUser) {
        whenever(userApiService.createUser(firstName, lastName, email, groupCodes)).thenReturn(uuid)

        whenever(userGroupApiService.getUserGroups(uuid, false)).thenReturn(listOf(UserGroup("NON PECS Groups", "NON PECS Test Group")))
        whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(
          createAuthServiceWith(
            "prison-staff-hub",
            "service-non-pecs@testing.com",
          ),
        )

        userService.createUser(newUser)

        verify(authApiService).syncExternalUserCreate(email, firstName, lastName)
      }
    }

    @Test
    fun `should not sync user creation with Auth when user update sync disabled`() {
      val newUser = NewUser(emailAddress, "Testy", "McTester", setOf("SITE_1_GROUP_1"))
      val uuid = UUID.randomUUID()
      with(newUser) {
        whenever(userApiService.createUser(firstName, lastName, email, groupCodes)).thenReturn(uuid)

        whenever(userGroupApiService.getUserGroups(uuid, false)).thenReturn(
          listOf(
            UserGroup(
              "NON PECS Groups",
              "NON PECS Test Group",
            ),
          ),
        )
        whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(
          createAuthServiceWith(
            "prison-staff-hub",
            "service-non-pecs@testing.com",
          ),
        )

        userService.createUser(newUser)

        verify(authApiService, never()).syncExternalUserCreate(email, firstName, lastName)
      }
    }
  }

  @Nested
  inner class EnableExternalUser {

    @Test
    fun `updates external user on success`() {
      val externalUser = createExternalUserDetails(userId = userUUID)
      whenever(externalUsersSearchApiService.findByUserId(userUUID)).thenReturn(externalUser)

      userService.enableUserByUserId(userUUID)

      verify(userApiService).enableUserById(userUUID)
    }

    @Test
    fun `sends email on success`() {
      val externalUser = createExternalUserDetails(userId = userUUID)
      whenever(externalUsersSearchApiService.findByUserId(userUUID)).thenReturn(externalUser)

      userService.enableUserByUserId(userUUID)

      verify(notificationService).externalUserEnabledNotification(externalUser)
    }

    @Test
    fun `does not send email on failure`() {
      doThrow(WebClientResponseException::class).whenever(userApiService).enableUserById(userUUID)

      assertThatThrownBy {
        userService.enableUserByUserId(userUUID)
      }.isInstanceOf(WebClientResponseException::class.java)

      verifyNoInteractions(notificationService)
    }

    @Test
    fun `syncs update with Auth when sync user updates feature enabled`() {
      givenAuthUserSyncEnabled()
      val externalUser = createExternalUserDetails(userId = userUUID)
      whenever(externalUsersSearchApiService.findByUserId(userUUID)).thenReturn(externalUser)

      userService.enableUserByUserId(userUUID)

      verify(authApiService).syncUserEnabled(externalUser.username)
    }

    @Test
    fun `does not sync updates with Auth when sync user updates feature disabled`() {
      val externalUser = createExternalUserDetails(userId = userUUID)
      whenever(externalUsersSearchApiService.findByUserId(userUUID)).thenReturn(externalUser)

      userService.enableUserByUserId(userUUID)

      verifyNoInteractions(authApiService)
    }
  }

  @Nested
  inner class DisableExternalUser {

    @Test
    fun `updates external user on success`() {
      val reason = DeactivateReason("Fired")
      userService.disableUserByUserId(userUUID, reason)

      verify(userApiService).disableUserById(userUUID, reason)
    }

    @Test
    fun `syncs update with Auth when sync user updates feature enabled`() {
      givenAuthUserSyncEnabled()
      val reason = DeactivateReason("Fired")
      val externalUser = createExternalUserDetails(userId = userUUID)
      whenever(externalUsersSearchApiService.findByUserId(userUUID)).thenReturn(externalUser)

      userService.disableUserByUserId(userUUID, reason)

      verify(authApiService).syncUserDisabled(externalUser.username, "Fired")
    }

    @Test
    fun `does not sync updates with Auth when sync user updates feature disabled`() {
      val reason = DeactivateReason("Fired")

      userService.disableUserByUserId(userUUID, reason)

      verifyNoInteractions(authApiService)
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
    fun `user with password - success link returned`() {
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
    fun `user with password - email and username updated`() {
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
    fun `user with password - email and username synced with auth when feature enabled`() {
      givenAuthUserSyncEnabled()
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(true)
      whenever(verifyEmailService.requestVerification(eq(externalUser), eq(newEmailAddress))).thenReturn(
        LinkEmailAndUsername("link", newEmailAddress, "testing"),
      )

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(authApiService).syncUserEmailUpdate(externalUser.username, newEmailAddress, "testing")
    }

    @Test
    fun `user with password - email and username not synced with auth when feature disabled`() {
      val externalUser = createExternalUserDetails(userId)

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(true)
      whenever(verifyEmailService.requestVerification(eq(externalUser), eq(newEmailAddress))).thenReturn(
        LinkEmailAndUsername("link", newEmailAddress, "testing"),
      )

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verifyNoInteractions(authApiService)
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

      verify(notificationService).externalUserInitialNotification(
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq("service-pecs@testing.com"),
        anyString(),
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

      verify(notificationService).externalUserInitialNotification(
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq("service-not-pecs@testing.com"),
        anyString(),
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

      verify(notificationService).externalUserInitialNotification(
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq("service-pecs@testing.com"),
        anyString(),
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

      verify(notificationService).externalUserInitialNotification(
        any(),
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        eq("service-not-pecs@testing.com"),
        anyString(),
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

      verify(notificationService).externalUserInitialNotification(userId, externalUser.firstName, externalUser.lastName, externalUser.username, newEmailAddress, "service-not-pecs@testing.com", "ExternalUserAmend")
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
    fun `user without password - email and username synced with Auth when feature enabled`() {
      givenAuthUserSyncEnabled()
      val externalUser = createExternalUserDetails(userId, "TESTY@TESTING.COM")

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      whenever(authApiService.createResetTokenForUser(userId)).thenReturn(token)
      whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(listOf())
      whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(createAuthServiceWith("service-not-pecs@testing.com", "prison-staff-hub"))
      whenever(verifyEmailService.confirmUsernameValidForUpdate(newEmailAddress, "TESTY@TESTING.COM")).thenReturn(newEmailAddress)

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(authApiService).syncUserEmailUpdate(externalUser.username, newEmailAddress, newEmailAddress)
    }

    @Test
    fun `user without password - email and username not synced with Auth when feature disabled`() {
      val externalUser = createExternalUserDetails(userId, "TESTY@TESTING.COM")

      whenever(externalUsersSearchApiService.findByUserId(userId)).thenReturn(externalUser)
      whenever(userApiService.hasPassword(userId)).thenReturn(false)
      whenever(authApiService.createResetTokenForUser(userId)).thenReturn(token)
      whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(listOf())
      whenever(authApiService.findServiceByServiceCode("prison-staff-hub")).thenReturn(createAuthServiceWith("service-not-pecs@testing.com", "prison-staff-hub"))
      whenever(verifyEmailService.confirmUsernameValidForUpdate(newEmailAddress, "TESTY@TESTING.COM")).thenReturn(newEmailAddress)

      userService.amendUserEmailByUserId(userId, newEmailAddress)

      verify(authApiService, never()).syncUserEmailUpdate(externalUser.username, newEmailAddress, newEmailAddress)
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
  }

  private fun givenAuthUserSyncEnabled() {
    userService = UserService(
      notificationService,
      userApiService,
      externalUsersSearchApiService,
      authApiService,
      userGroupApiService,
      verifyEmailService,
      telemetryClient,
      hmppsAuthenticationHolder,
      true,
    )
  }

  private fun createAuthServiceWith(contact: String, code: String): AuthService = AuthService(code, "name", "desc", contact, "url")
}

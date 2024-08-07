package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.email.NotificationService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonAdminUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserSearchSummary
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailAddress
import uk.gov.justice.digital.hmpps.manageusersapi.model.EnhancedPrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonStaffUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.filter.PrisonUserFilter
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PageDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PageSort
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedCentralAdminUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedGeneralUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedLocalAdminUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType.DPS_GEN
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.EntityNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.LinkEmailAndUsername
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailService

class UserServiceTest {

  private val prisonUserApiService: UserApiService = mock()
  private val authApiService: AuthApiService = mock()
  private val notificationService: NotificationService = mock()
  private val verifyEmailDomainService: VerifyEmailDomainService = mock()
  private val verifyEmailService: VerifyEmailService = mock()
  private val prisonUserService = UserService(
    prisonUserApiService,
    authApiService,
    notificationService,
    verifyEmailDomainService,
    verifyEmailService,
  )

  @Nested
  inner class ChangeEmail {
    private val userName = "testy"
    private val newEmailAddress = "new.testy@testing.com"

    @Test
    fun `should throw exception when prison user not present in prison system`() {
      whenever(prisonUserApiService.findUserByUsername(userName)).thenReturn(null)

      assertThatThrownBy { prisonUserService.changeEmail(userName, newEmailAddress) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Prison username $userName not found")

      verify(authApiService, never()).confirmRecognised(any())
      verify(verifyEmailService, never()).requestVerification(any(), any())
      verify(authApiService, never()).updateEmail(any(), any())
    }

    @Test
    fun `should throw exception when user not recognised by Auth`() {
      whenever(prisonUserApiService.findUserByUsername(userName)).thenReturn(createPrisonUserDetails())
      doThrow(RuntimeException("Auth API call failed")).whenever(authApiService).confirmRecognised(userName)

      assertThatThrownBy { prisonUserService.changeEmail(userName, newEmailAddress) }
        .isInstanceOf(RuntimeException::class.java)
        .hasMessage("Auth API call failed")

      verify(verifyEmailService, never()).requestVerification(any(), any())
      verify(authApiService, never()).updateEmail(any(), any())
    }

    @Test
    fun `should request verification of new email address`() {
      val prisonUser = createPrisonUserDetails()
      whenever(prisonUserApiService.findUserByUsername(userName)).thenReturn(prisonUser)
      whenever(verifyEmailService.requestVerification(prisonUser, newEmailAddress)).thenReturn(
        LinkEmailAndUsername("link", newEmailAddress, userName),
      )

      prisonUserService.changeEmail(userName, newEmailAddress)

      verify(verifyEmailService).requestVerification(prisonUser, newEmailAddress)
    }

    @Test
    fun `should update email address in Auth`() {
      val prisonUser = createPrisonUserDetails()
      whenever(prisonUserApiService.findUserByUsername(userName)).thenReturn(prisonUser)
      whenever(verifyEmailService.requestVerification(prisonUser, newEmailAddress)).thenReturn(
        LinkEmailAndUsername("link", newEmailAddress, userName),
      )

      prisonUserService.changeEmail(userName, newEmailAddress)

      verify(authApiService).updateEmail(userName, newEmailAddress)
    }

    @Test
    fun `should respond with verify link`() {
      val prisonUser = createPrisonUserDetails()
      whenever(prisonUserApiService.findUserByUsername(userName)).thenReturn(prisonUser)
      whenever(verifyEmailService.requestVerification(prisonUser, newEmailAddress)).thenReturn(
        LinkEmailAndUsername("link", newEmailAddress, userName),
      )

      val verifyLink = prisonUserService.changeEmail(userName, newEmailAddress)

      assertThat(verifyLink).isEqualTo("link")
    }
  }

  @Nested
  inner class CreateUser {
    @Test
    fun `create a DPS central admin user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", DPS_ADM)

      whenever(prisonUserApiService.createCentralAdminUser(user)).thenReturn(
        PrisonUser(
          user.username,
          user.firstName,
          102,
          user.lastName,
          "CADM_I",
          user.email,
          true,
          listOf(),
        ),
      )
      prisonUserService.createUser(user)
      verify(prisonUserApiService).createCentralAdminUser(user)
      verify(notificationService).newPrisonUserNotification(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS general user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", DPS_GEN, "MDI")
      whenever(prisonUserApiService.createGeneralUser(user)).thenReturn(
        PrisonUser(
          user.username,
          user.firstName,
          101,
          user.lastName,
          "BXI",
          user.email,
          true,
          listOf(),
        ),
      )
      prisonUserService.createUser(user)
      verify(prisonUserApiService).createGeneralUser(user)
      verify(notificationService).newPrisonUserNotification(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS local admin user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", DPS_LSA, "MDI")
      whenever(prisonUserApiService.createLocalAdminUser(user)).thenReturn(
        PrisonUser(
          user.username,
          user.firstName,
          100,
          user.lastName,
          "CADM_I",
          user.email,
          true,
          listOf(),
        ),
      )
      prisonUserService.createUser(user)
      verify(prisonUserApiService).createLocalAdminUser(user)
      verify(notificationService).newPrisonUserNotification(user, "DPSUserCreate")
    }

    @Test
    fun `should validate email domain`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(false)
      val userWithInvalidEmailDomain = CreateUserRequest(
        "CEN_ADM",
        "cadmin@test.gov.uk",
        "First",
        "Last",
        DPS_LSA,
        "MDI",
      )

      assertThatThrownBy { prisonUserService.createUser(userWithInvalidEmailDomain) }
        .isInstanceOf(HmppsValidationException::class.java)
        .hasMessage("Invalid Email domain: test.gov.uk with reason: Email domain not valid")
    }
  }

  @Nested
  inner class CreateLinkedCentralAdminAccountToAnExistingGeneralAccount {
    @Test
    fun `create a DPS central admin user linked to a General account`() {
      val createLinkedCentralAdminUserRequest = CreateLinkedCentralAdminUserRequest("TEST_USER", "TEST_USER_ADM")
      val prisonStaffUser = UserFixture.createPrisonStaffUser()
      val createUserRequest =
        createUserRequest(createLinkedCentralAdminUserRequest.adminUsername, DPS_ADM, prisonStaffUser)
      whenever(prisonUserApiService.linkCentralAdminUser(createLinkedCentralAdminUserRequest)).thenReturn(
        prisonStaffUser,
      )
      prisonUserService.createLinkedCentralAdminUser(createLinkedCentralAdminUserRequest)
      verify(prisonUserApiService).linkCentralAdminUser(createLinkedCentralAdminUserRequest)
      if (createUserRequest != null) {
        verify(notificationService).newPrisonUserNotification(createUserRequest, "DPLinkUserCreate")
      }
    }
  }

  private fun createUserRequest(adminUser: String, userType: UserType, user: PrisonStaffUser): CreateUserRequest? {
    val createUserRequest = user.primaryEmail?.let {
      CreateUserRequest(
        username = adminUser,
        email = it,
        firstName = user.firstName,
        lastName = user.lastName,
        userType = userType,
      )
    }
    return createUserRequest
  }

  @Nested
  inner class CreateLinkedLocalAdminAccountToAnExistingGeneralAccount {
    @Test
    fun `create a DPS Local admin user linked to a General account`() {
      val createLinkedLocalAdminUserRequest = CreateLinkedLocalAdminUserRequest("TEST_USER", "TEST_USER_ADM", "MDI")
      val prisonStaffUser = UserFixture.createPrisonStaffUser()
      whenever(prisonUserApiService.linkLocalAdminUser(createLinkedLocalAdminUserRequest)).thenReturn(
        prisonStaffUser,
      )
      val createUserRequest =
        createUserRequest(createLinkedLocalAdminUserRequest.adminUsername, DPS_LSA, prisonStaffUser)
      prisonUserService.createLinkedLocalAdminUser(createLinkedLocalAdminUserRequest)
      verify(prisonUserApiService).linkLocalAdminUser(createLinkedLocalAdminUserRequest)
      if (createUserRequest != null) {
        verify(notificationService).newPrisonUserNotification(createUserRequest, "DPLinkUserCreate")
      }
    }
  }

  @Nested
  inner class CreateLinkedGeneralAccountToAnExistingAdminAccount {
    @Test
    fun `create a DPS General user linked to an Admin account`() {
      val createLinkedGeneralUserRequest = CreateLinkedGeneralUserRequest("TEST_USER_ADM", "TEST_USER_GEN", "BXI")
      val prisonStaffUser = UserFixture.createPrisonStaffUser()
      whenever(prisonUserApiService.linkGeneralUser(createLinkedGeneralUserRequest)).thenReturn(
        prisonStaffUser,
      )
      val createUserRequest =
        createUserRequest(createLinkedGeneralUserRequest.generalUsername, DPS_GEN, prisonStaffUser)
      prisonUserService.createLinkedGeneralUser(createLinkedGeneralUserRequest)
      verify(prisonUserApiService).linkGeneralUser(createLinkedGeneralUserRequest)
      if (createUserRequest != null) {
        verify(notificationService).newPrisonUserNotification(createUserRequest, "DPLinkUserCreate")
      }
    }
  }

  @Nested
  inner class FindPrisonUsersByFirstAndLastNames {
    @Test
    fun `no matches`() {
      whenever(prisonUserService.findUsersByFirstAndLastName("first", "last")).thenReturn(listOf())

      assertThat(prisonUserService.findUsersByFirstAndLastName("first", "last")).isEmpty()
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `prison users only`() {
      whenever(prisonUserApiService.findUsersByFirstAndLastName("first", "last")).thenReturn(
        listOf(
          PrisonUserSummary("U1", "1", "F1", "l1", false, null, "u1@justice.gov.uk"),
          PrisonUserSummary("U2", "2", "F2", "l2", false, null, null),
          PrisonUserSummary("U3", "3", "F3", "l3", false, PrisonCaseload("MDI", "Moorland"), null),
        ),
      )
      whenever(authApiService.findUserEmails(listOf())).thenReturn(listOf())

      assertThat(prisonUserService.findUsersByFirstAndLastName("first", "last"))
        .containsExactlyInAnyOrder(
          EnhancedPrisonUser(
            username = "U1",
            email = "u1@justice.gov.uk",
            verified = true,
            userId = "1",
            firstName = "F1",
            lastName = "l1",
            activeCaseLoadId = null,
          ),
          EnhancedPrisonUser(
            username = "U2",
            email = null,
            verified = false,
            userId = "2",
            firstName = "F2",
            lastName = "l2",
            activeCaseLoadId = null,
          ),
          EnhancedPrisonUser(
            username = "U3",
            email = null,
            verified = false,
            userId = "3",
            firstName = "F3",
            lastName = "l3",
            activeCaseLoadId = "MDI",
          ),
        )
    }

    @Test
    fun `Prison users matched in auth`() {
      whenever(prisonUserApiService.findUsersByFirstAndLastName("first", "last")).thenReturn(
        listOf(
          PrisonUserSummary("U1", "1", "F1", "l1", false, PrisonCaseload("MDI", "Moorland"), null),
          PrisonUserSummary("U2", "2", "F2", "l2", false, null, null),
          PrisonUserSummary("U3", "3", "F3", "l3", false, PrisonCaseload("MDI", "Moorland"), null),
        ),
      )

      whenever(authApiService.findUserEmails(ArgumentMatchers.anyList())).thenReturn(
        listOf(
          EmailAddress(username = "U1", email = "u1@b.com", verified = true),
          EmailAddress(username = "U2", email = "u2@b.com", verified = true),
          EmailAddress(username = "U3", email = "u3@b.com", verified = false),
        ),
      )

      assertThat(prisonUserService.findUsersByFirstAndLastName("first", "last").size).isEqualTo(3)
      assertThat(prisonUserService.findUsersByFirstAndLastName("first", "last"))
        .containsExactlyInAnyOrder(
          EnhancedPrisonUser(
            username = "U1",
            email = "u1@b.com",
            verified = true,
            userId = "1",
            firstName = "F1",
            lastName = "l1",
            activeCaseLoadId = "MDI",
          ),
          EnhancedPrisonUser(
            username = "U2",
            email = "u2@b.com",
            verified = true,
            userId = "2",
            firstName = "F2",
            lastName = "l2",
            activeCaseLoadId = null,
          ),
          EnhancedPrisonUser(
            username = "U3",
            email = "u3@b.com",
            verified = false,
            userId = "3",
            firstName = "F3",
            lastName = "l3",
            activeCaseLoadId = "MDI",
          ),
        )
    }

    @Test
    fun `Prison users partially matched in auth`() {
      whenever(prisonUserApiService.findUsersByFirstAndLastName("first", "last")).thenReturn(
        listOf(
          PrisonUserSummary("U1", "1", "F1", "l1", false, PrisonCaseload("MDI", "Moorland"), null),
          PrisonUserSummary("U2", "2", "F2", "l2", false, null, "u2@justice.gov.uk"),
          PrisonUserSummary("U3", "3", "F3", "l3", false, null, "u3@justice.gov.uk"),
          PrisonUserSummary("U4", "4", "F4", "l4", false, PrisonCaseload("MDI", "Moorland"), null),
        ),
      )

      whenever(authApiService.findUserEmails(ArgumentMatchers.anyList())).thenReturn(
        listOf(
          EmailAddress(username = "U1", email = "u1@b.com", verified = true),
          // User U2 in auth, but no email - so search NOMIS for e-mail for this user
          EmailAddress(username = "U2", email = null, verified = true),
        ),
      )

      assertThat(prisonUserService.findUsersByFirstAndLastName("first", "last"))
        .containsExactlyInAnyOrder(
          EnhancedPrisonUser(
            username = "U1",
            email = "u1@b.com",
            verified = true,
            userId = "1",
            firstName = "F1",
            lastName = "l1",
            activeCaseLoadId = "MDI",
          ),
          EnhancedPrisonUser(
            username = "U2",
            email = "u2@justice.gov.uk",
            verified = true,
            userId = "2",
            firstName = "F2",
            lastName = "l2",
            activeCaseLoadId = null,
          ),
          EnhancedPrisonUser(
            username = "U3",
            email = "u3@justice.gov.uk",
            verified = true,
            userId = "3",
            firstName = "F3",
            lastName = "l3",
            activeCaseLoadId = null,
          ),
          EnhancedPrisonUser(
            username = "U4",
            email = null,
            verified = false,
            userId = "4",
            firstName = "F4",
            lastName = "l4",
            activeCaseLoadId = "MDI",
          ),
        )
    }
  }

  @Nested
  inner class FindPrisonUsersByUsername {
    @Test
    fun `find prison user by username`() {
      whenever(prisonUserApiService.findUserByUsernameWithError("NUSER_GEN")).thenReturn(
        createPrisonUserDetails(),
      )
      assertThat(prisonUserService.findUserByUsername("NUSER_GEN")).isEqualTo(
        createPrisonUserDetails(),
      )
    }
  }

  @Nested
  inner class EnablePrisonUser {
    @Test
    fun `updates prison user on success`() {
      val prisonuser = UserFixture.createPrisonUserDetails()

      doNothing().whenever(prisonUserApiService).enableUserByUserId(prisonuser.username)
      whenever(prisonUserApiService.findUserByUsername(prisonuser.username)).thenReturn(prisonuser)

      prisonUserService.enableUser(prisonuser.username)

      // verify prisonUserApiService.enableUserByUserId is called
      verify(prisonUserApiService).enableUserByUserId(prisonuser.username)
    }

    @Test
    fun `throws error if prison user doesn't exist`() {
      val prisonuser = UserFixture.createPrisonUserDetails()
      whenever(prisonUserApiService.findUserByUsername(prisonuser.username)).thenReturn(null)

      // verify that the exception is thrown
      assertThatThrownBy { prisonUserService.enableUser(prisonuser.username) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Prison username ${prisonuser.username} not found")
    }
  }

  @Nested
  inner class DisablePrisonUser {
    @Test
    fun `updates prison user on success`() {
      val prisonuser = UserFixture.createPrisonUserDetails()

      doNothing().whenever(prisonUserApiService).disableUserByUserId(prisonuser.username)
      whenever(prisonUserApiService.findUserByUsername(prisonuser.username)).thenReturn(prisonuser)

      prisonUserService.disableUser(prisonuser.username)

      // verify prisonUserApiService.enableUserByUserId is called
      verify(prisonUserApiService).disableUserByUserId(prisonuser.username)
    }

    @Test
    fun `throws error if prison user doesn't exist`() {
      val prisonuser = UserFixture.createPrisonUserDetails()
      whenever(prisonUserApiService.findUserByUsername(prisonuser.username)).thenReturn(null)

      // verify that the exception is thrown
      assertThatThrownBy { prisonUserService.disableUser(prisonuser.username) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Prison username ${prisonuser.username} not found")
    }
  }

  @Nested
  inner class FindUsersByFilter {
    @Test
    fun `calls prisonUserApiService`() {
      val sort = PageSort(true, false, true)
      val response = PagedResponse(
        content = listOf(
          createPrisonUserSearchSummary(username = "user1"),
          createPrisonUserSearchSummary(username = "user2"),
        ),
        pageable = PageDetails(sort, 10, 1, 2, true, true),
        totalElements = 2,
        totalPages = 1,
        last = true,
        first = true,
        sort = sort,
        numberOfElements = 2,
        size = 2,
        number = 0,
        empty = false,
      )
      whenever(prisonUserApiService.findUsersByFilter(any(), any())).thenReturn(response)

      prisonUserService.findUsersByFilter(PageRequest.of(0, 10), PrisonUserFilter())

      verify(prisonUserApiService).findUsersByFilter(PageRequest.of(0, 10), PrisonUserFilter())
    }
  }

  @Nested
  inner class DownloadUsersByFilter {
    @Test
    fun `calls prisonUserApiService`() {
      whenever(prisonUserApiService.downloadUsersByFilter(any())).thenReturn(
        listOf(
          createPrisonUserSummary(username = "user1"),
          createPrisonUserSummary(username = "user2"),
        ),
      )

      prisonUserService.downloadUsersByFilter(PrisonUserFilter())

      verify(prisonUserApiService).downloadUsersByFilter(PrisonUserFilter())
    }
  }

  @Nested
  inner class DownloadPrisonAdminsByFilter {
    @Test
    fun `calls prisonUserApiService`() {
      whenever(prisonUserApiService.downloadPrisonAdminsByFilter(any())).thenReturn(
        listOf(
          createPrisonAdminUserSummary(username = "user1"),
          createPrisonAdminUserSummary(username = "user2"),
        ),
      )

      prisonUserService.downloadPrisonAdminsByFilter(PrisonUserFilter())

      verify(prisonUserApiService).downloadPrisonAdminsByFilter(PrisonUserFilter())
    }
  }
}

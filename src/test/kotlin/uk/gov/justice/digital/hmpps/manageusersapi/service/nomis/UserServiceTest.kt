package uk.gov.justice.digital.hmpps.manageusersapi.service.nomis

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailAddress
import uk.gov.justice.digital.hmpps.manageusersapi.model.NewPrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.NomisUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType
import uk.gov.justice.digital.hmpps.manageusersapi.service.TokenService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService

class UserServiceTest {
  private val nomisUserApiService: UserApiService = mock()
  private val authApiService: AuthApiService = mock()
  private val tokenService: TokenService = mock()
  private val verifyEmailDomainService: VerifyEmailDomainService = mock()
  private val nomisUserService = UserService(
    nomisUserApiService,
    authApiService,
    tokenService,
    verifyEmailDomainService,
  )

  @Nested
  inner class CreateUser {
    @Test
    fun `create a DPS central admin user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_ADM)

      whenever(nomisUserApiService.createCentralAdminUser(user)).thenReturn(
        NewPrisonUser(
          user.username,
          user.email,
          user.firstName,
          user.lastName,
        )
      )
      nomisUserService.createUser(user)
      verify(nomisUserApiService).createCentralAdminUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS general user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_GEN, "MDI")
      whenever(nomisUserApiService.createGeneralUser(user)).thenReturn(
        NewPrisonUser(
          user.username,
          user.email,
          user.firstName,
          user.lastName,
        )
      )
      nomisUserService.createUser(user)
      verify(nomisUserApiService).createGeneralUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `create a DPS local admin user`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(true)
      val user = CreateUserRequest("CEN_ADM", "cadmin@gov.uk", "First", "Last", UserType.DPS_LSA, "MDI")
      whenever(nomisUserApiService.createLocalAdminUser(user)).thenReturn(
        NewPrisonUser(
          user.username,
          user.email,
          user.firstName,
          user.lastName,
        )
      )
      nomisUserService.createUser(user)
      verify(nomisUserApiService).createLocalAdminUser(user)
      verify(tokenService).saveAndSendInitialEmail(user, "DPSUserCreate")
    }

    @Test
    fun `should validate email domain`() {
      whenever(verifyEmailDomainService.isValidEmailDomain(any())).thenReturn(false)
      val userWithInvalidEmailDomain = CreateUserRequest(
        "CEN_ADM",
        "cadmin@test.gov.uk",
        "First",
        "Last",
        UserType.DPS_LSA,
        "MDI"
      )

      assertThatThrownBy { nomisUserService.createUser(userWithInvalidEmailDomain) }
        .isInstanceOf(HmppsValidationException::class.java)
        .hasMessage("Invalid Email domain: test.gov.uk with reason: Email domain not valid")
    }
  }

  @Nested
  inner class FindPrisonUsersByFirstAndLastNames {
    @Test
    fun `no matches`() {
      whenever(nomisUserService.findUsersByFirstAndLastName("first", "last")).thenReturn(listOf())

      assertThat(nomisUserService.findUsersByFirstAndLastName("first", "last")).isEmpty()
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `prison users only`() {
      whenever(nomisUserApiService.findUsersByFirstAndLastName("first", "last")).thenReturn(
        listOf(
          NomisUserSummary("U1", "1", "F1", "l1", false, null, "u1@justice.gov.uk"),
          NomisUserSummary("U2", "2", "F2", "l2", false, null, null),
          NomisUserSummary("U3", "3", "F3", "l3", false, PrisonCaseload("MDI", "Moorland"), null)
        )
      )
      whenever(authApiService.findUserEmails(listOf())).thenReturn(listOf())

      assertThat(nomisUserService.findUsersByFirstAndLastName("first", "last"))
        .containsExactlyInAnyOrder(
          PrisonUser(
            username = "U1",
            email = "u1@justice.gov.uk",
            verified = true,
            userId = "1",
            firstName = "F1",
            lastName = "l1",
            activeCaseLoadId = null
          ),
          PrisonUser(
            username = "U2",
            email = null,
            verified = false,
            userId = "2",
            firstName = "F2",
            lastName = "l2",
            activeCaseLoadId = null
          ),
          PrisonUser(
            username = "U3",
            email = null,
            verified = false,
            userId = "3",
            firstName = "F3",
            lastName = "l3",
            activeCaseLoadId = "MDI"
          ),
        )
    }

    @Test
    fun `Prison users matched in auth`() {
      whenever(nomisUserApiService.findUsersByFirstAndLastName("first", "last")).thenReturn(
        listOf(
          NomisUserSummary("U1", "1", "F1", "l1", false, PrisonCaseload("MDI", "Moorland"), null),
          NomisUserSummary("U2", "2", "F2", "l2", false, null, null),
          NomisUserSummary("U3", "3", "F3", "l3", false, PrisonCaseload("MDI", "Moorland"), null)
        )
      )

      whenever(authApiService.findUserEmails(ArgumentMatchers.anyList())).thenReturn(
        listOf(
          EmailAddress(username = "U1", email = "u1@b.com", verified = true),
          EmailAddress(username = "U2", email = "u2@b.com", verified = true),
          EmailAddress(username = "U3", email = "u3@b.com", verified = false)
        )
      )

      assertThat(nomisUserService.findUsersByFirstAndLastName("first", "last").size).isEqualTo(3)
      assertThat(nomisUserService.findUsersByFirstAndLastName("first", "last"))
        .containsExactlyInAnyOrder(
          PrisonUser(
            username = "U1",
            email = "u1@b.com",
            verified = true,
            userId = "1",
            firstName = "F1",
            lastName = "l1",
            activeCaseLoadId = "MDI"
          ),
          PrisonUser(
            username = "U2",
            email = "u2@b.com",
            verified = true,
            userId = "2",
            firstName = "F2",
            lastName = "l2",
            activeCaseLoadId = null

          ),
          PrisonUser(
            username = "U3",
            email = "u3@b.com",
            verified = false,
            userId = "3",
            firstName = "F3",
            lastName = "l3",
            activeCaseLoadId = "MDI"
          ),
        )
    }

    @Test
    fun `Prison users partially matched in auth`() {

      whenever(nomisUserApiService.findUsersByFirstAndLastName("first", "last")).thenReturn(
        listOf(
          NomisUserSummary("U1", "1", "F1", "l1", false, PrisonCaseload("MDI", "Moorland"), null),
          NomisUserSummary("U2", "2", "F2", "l2", false, null, "u2@justice.gov.uk"),
          NomisUserSummary("U3", "3", "F3", "l3", false, null, "u3@justice.gov.uk"),
          NomisUserSummary("U4", "4", "F4", "l4", false, PrisonCaseload("MDI", "Moorland"), null)
        )
      )

      whenever(authApiService.findUserEmails(ArgumentMatchers.anyList())).thenReturn(
        listOf(
          EmailAddress(username = "U1", email = "u1@b.com", verified = true),
          // User U2 in auth, but no email - so search NOMIS for e-mail for this user
          EmailAddress(username = "U2", email = null, verified = true)
        )
      )

      assertThat(nomisUserService.findUsersByFirstAndLastName("first", "last"))
        .containsExactlyInAnyOrder(
          PrisonUser(
            username = "U1",
            email = "u1@b.com",
            verified = true,
            userId = "1",
            firstName = "F1",
            lastName = "l1",
            activeCaseLoadId = "MDI"
          ),
          PrisonUser(
            username = "U2",
            email = "u2@justice.gov.uk",
            verified = true,
            userId = "2",
            firstName = "F2",
            lastName = "l2",
            activeCaseLoadId = null
          ),
          PrisonUser(
            username = "U3",
            email = "u3@justice.gov.uk",
            verified = true,
            userId = "3",
            firstName = "F3",
            lastName = "l3",
            activeCaseLoadId = null
          ),
          PrisonUser(
            username = "U4",
            email = null,
            verified = false,
            userId = "4",
            firstName = "F4",
            lastName = "l4",
            activeCaseLoadId = "MDI"
          ),
        )
    }
  }
}

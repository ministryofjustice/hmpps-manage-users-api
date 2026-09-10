package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserFullDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserDetailsList
import uk.gov.justice.digital.hmpps.manageusersapi.service.EntityNotFoundException

class UserApiServiceTest {

  private val serviceWebClientUtils: WebClientUtils = mock()
  private val userWebClientUtils: WebClientUtils = mock()
  private val userExtendedTimeoutWebClientUtils: WebClientUtils = mock()

  private val userApiService = UserApiService(
    serviceWebClientUtils,
    userWebClientUtils,
    userExtendedTimeoutWebClientUtils,
  )

  @Nested
  inner class FindUserDetailsByEmail {
    @Test
    fun `returns user details list when email contains at symbol`() {
      val email = "bob@justice.gov.uk"
      val expected = PrisonUserDetailsList().apply { add(createPrisonUserFullDetails()) }
      whenever(
        serviceWebClientUtils.getWithParams(
          "/users/user",
          PrisonUserDetailsList::class.java,
          mapOf("email" to email),
        ),
      ).thenReturn(expected)

      val actual = userApiService.findUserDetailsByEmail(email)

      assertThat(actual).isEqualTo(expected)
      verify(serviceWebClientUtils).getWithParams(
        "/users/user",
        PrisonUserDetailsList::class.java,
        mapOf("email" to email),
      )
    }

    @Test
    fun `throws entity not found when email does not contain at symbol`() {
      val invalidEmail = "bob.justice.gov.uk"

      assertThatThrownBy { userApiService.findUserDetailsByEmail(invalidEmail) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Prison user email $invalidEmail not allowed")

      verifyNoInteractions(serviceWebClientUtils)
    }

    @Test
    fun `passes through empty result when no users match email`() {
      val email = "missing@justice.gov.uk"
      val expected = PrisonUserDetailsList()
      whenever(
        serviceWebClientUtils.getWithParams(
          "/users/user",
          PrisonUserDetailsList::class.java,
          mapOf("email" to email),
        ),
      ).thenReturn(expected)

      val actual = userApiService.findUserDetailsByEmail(email)

      assertThat(actual).isEqualTo(expected)
      verify(serviceWebClientUtils).getWithParams(
        "/users/user",
        PrisonUserDetailsList::class.java,
        mapOf("email" to email),
      )
    }
  }

  @Nested
  inner class FindUserDetailsByUsername {
    @Test
    fun `returns user details when username is valid`() {
      val username = "nuser_gen"
      val expected: PrisonUserDetails = createPrisonUserFullDetails()
      whenever(serviceWebClientUtils.get("/users/{username}", PrisonUserDetails::class.java, username.uppercase())).thenReturn(expected)

      val actual = userApiService.findUserDetailsByUsername(username)

      assertThat(actual).isEqualTo(expected)
      verify(serviceWebClientUtils).get("/users/{username}", PrisonUserDetails::class.java, username.uppercase())
    }

    @Test
    fun `throws entity not found when username contains at symbol`() {
      val invalidUsername = "user@name"

      assertThatThrownBy { userApiService.findUserDetailsByUsername(invalidUsername) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Prison username $invalidUsername not allowed")

      verifyNoInteractions(serviceWebClientUtils)
    }
  }
}

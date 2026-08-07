package uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthService
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.AzureUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailAddress
import uk.gov.justice.digital.hmpps.manageusersapi.resource.AccessPeriod
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserAllowlistAddRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserAllowlistDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserAllowlistPatchRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserAllowlistUserType
import uk.gov.justice.digital.hmpps.manageusersapi.service.Status
import java.util.UUID
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.Status as AllowListStatus

class AuthApiServiceTest {

  private val serviceWebClientUtils: WebClientUtils = mock()
  private val userWebClientUtils: WebClientUtils = mock()

  private val authApiService = AuthApiService(serviceWebClientUtils, userWebClientUtils)

  @Nested
  inner class CreateNewToken {
    @Test
    fun `passes correct URI and body parameters to webclient`() {
      val request = CreateTokenRequest(
        username = "user1",
        email = "user1@example.com",
        source = "auth",
        firstName = "First",
        lastName = "Last",
      )
      whenever(
        serviceWebClientUtils.postWithResponse(
          "/api/new-token",
          mapOf(
            "username" to "user1",
            "email" to "user1@example.com",
            "source" to "auth",
            "firstName" to "First",
            "lastName" to "Last",
          ),
          String::class.java,
        ),
      ).thenReturn("a-token")

      val result = authApiService.createNewToken(request)

      assertThat(result).isEqualTo("a-token")
      verify(serviceWebClientUtils).postWithResponse(
        "/api/new-token",
        mapOf(
          "username" to "user1",
          "email" to "user1@example.com",
          "source" to "auth",
          "firstName" to "First",
          "lastName" to "Last",
        ),
        String::class.java,
      )
    }
  }

  @Nested
  inner class CreateTokenByEmailType {
    @Test
    fun `passes correct URI and body parameters to webclient`() {
      val request = TokenByEmailTypeRequest(username = "user1", emailType = "PRIMARY")
      whenever(
        serviceWebClientUtils.postWithResponse(
          "/api/token/email-type",
          mapOf("username" to "user1", "emailType" to "PRIMARY"),
          String::class.java,
        ),
      ).thenReturn("email-token")

      val result = authApiService.createTokenByEmailType(request)

      assertThat(result).isEqualTo("email-token")
      verify(serviceWebClientUtils).postWithResponse(
        "/api/token/email-type",
        mapOf("username" to "user1", "emailType" to "PRIMARY"),
        String::class.java,
      )
    }
  }

  @Nested
  inner class CreateResetTokenForUser {
    @Test
    fun `passes correct URI and userId to webclient`() {
      val userId = UUID.randomUUID()
      whenever(serviceWebClientUtils.postWithResponse("/api/token/reset/{userId}", String::class.java, userId))
        .thenReturn("reset-token")

      val result = authApiService.createResetTokenForUser(userId)

      assertThat(result).isEqualTo("reset-token")
      verify(serviceWebClientUtils).postWithResponse("/api/token/reset/{userId}", String::class.java, userId)
    }
  }

  @Nested
  inner class FindAuthUserEmail {
    @Test
    fun `passes correct URI, username and unverified=false flag to webclient`() {
      val emailAddress = EmailAddress(username = "user1", email = "user1@example.com", verified = true)
      whenever(
        serviceWebClientUtils.getIgnoreError(
          "/api/user/{username}/authEmail?unverified={unverified}",
          EmailAddress::class.java,
          "user1",
          false,
        ),
      ).thenReturn(emailAddress)

      val result = authApiService.findAuthUserEmail("user1", false)

      assertThat(result).isEqualTo(emailAddress)
      verify(serviceWebClientUtils).getIgnoreError(
        "/api/user/{username}/authEmail?unverified={unverified}",
        EmailAddress::class.java,
        "user1",
        false,
      )
    }

    @Test
    fun `passes unverified=true when requested`() {
      authApiService.findAuthUserEmail("user1", true)

      verify(serviceWebClientUtils).getIgnoreError(
        "/api/user/{username}/authEmail?unverified={unverified}",
        EmailAddress::class.java,
        "user1",
        true,
      )
    }
  }

  @Nested
  inner class FindAzureUserByUsername {
    @Test
    fun `calls webclient with correct URI when username is a valid UUID`() {
      val uuid = UUID.randomUUID()

      authApiService.findAzureUserByUsername(uuid.toString())

      verify(serviceWebClientUtils).getIgnoreError(
        "/api/azureuser/{username}",
        AzureUser::class.java,
        uuid.toString(),
      )
    }

    @Test
    fun `returns null without calling webclient when username is not a valid UUID`() {
      val result = authApiService.findAzureUserByUsername("not-a-uuid")

      assertThat(result).isNull()
      verifyNoInteractions(serviceWebClientUtils)
    }
  }

  @Nested
  inner class FindServiceByServiceCode {
    @Test
    fun `passes correct URI and service code to webclient`() {
      authApiService.findServiceByServiceCode("SOME_SERVICE")

      verify(serviceWebClientUtils).get(
        "/api/services/{serviceCode}",
        AuthService::class.java,
        "SOME_SERVICE",
      )
    }
  }

  @Nested
  inner class FindUserIdByUsernameAndSource {
    @Test
    fun `passes correct URI, username and source to webclient`() {
      val authUser = AuthUser(uuid = UUID.randomUUID())
      whenever(
        serviceWebClientUtils.get(
          "/api/user/{username}/{source}",
          AuthUser::class.java,
          "user1",
          AuthSource.auth,
        ),
      ).thenReturn(authUser)

      val result = authApiService.findUserIdByUsernameAndSource("user1", AuthSource.auth)

      assertThat(result).isEqualTo(authUser)
      verify(serviceWebClientUtils).get(
        "/api/user/{username}/{source}",
        AuthUser::class.java,
        "user1",
        AuthSource.auth,
      )
    }
  }

  @Nested
  inner class SyncUserEmailUpdate {
    @Test
    fun `passes correct URI, body map and username to webclient`() {
      authApiService.syncUserEmailUpdate("user1", "new@example.com", "newuser1")

      verify(serviceWebClientUtils).putWithBody(
        mapOf("email" to "new@example.com", "username" to "newuser1"),
        "/api/externaluser/sync/{username}/email",
        "user1",
      )
    }
  }

  @Nested
  inner class SyncUserEnabled {
    @Test
    fun `passes enabled=true in body with correct URI and username to webclient`() {
      authApiService.syncUserEnabled("user1")

      verify(serviceWebClientUtils).putWithBody(
        mapOf("enabled" to true),
        "/api/externaluser/sync/{username}/enabled",
        "user1",
      )
    }
  }

  @Nested
  inner class SyncUserDisabled {
    @Test
    fun `passes enabled=false and inactiveReason in body with correct URI and username to webclient`() {
      authApiService.syncUserDisabled("user1", "LEFT_ORGANISATION")

      verify(serviceWebClientUtils).putWithBody(
        mapOf("enabled" to false, "inactiveReason" to "LEFT_ORGANISATION"),
        "/api/externaluser/sync/{username}/enabled",
        "user1",
      )
    }
  }

  @Nested
  inner class SyncExternalUserCreate {
    @Test
    fun `passes correct URI and body to webclient, using email as username`() {
      authApiService.syncExternalUserCreate("ext@example.com", "First", "Last")

      verify(serviceWebClientUtils).postWithBody(
        mapOf(
          "email" to "ext@example.com",
          "username" to "ext@example.com",
          "firstName" to "First",
          "lastName" to "Last",
        ),
        "/api/externaluser/sync/create",
      )
    }
  }

  @Nested
  inner class ConfirmRecognised {
    @Test
    fun `passes correct URI and username to userWebClientUtils`() {
      whenever(userWebClientUtils.getWithEmptyResponseSucceeds("/api/user/{username}/recognised", "user1"))
        .thenReturn(true)

      val result = authApiService.confirmRecognised("user1")

      assertThat(result).isTrue()
      verify(userWebClientUtils).getWithEmptyResponseSucceeds("/api/user/{username}/recognised", "user1")
    }
  }

  @Nested
  inner class SyncEmailWithNomis {
    @Test
    fun `passes correct URI, email body and username to userWebClientUtils`() {
      authApiService.syncEmailWithNomis("user1", "nomis@example.com")

      verify(userWebClientUtils).postWithBody(
        mapOf("email" to "nomis@example.com"),
        "/api/prisonuser/{username}/email/sync",
        "user1",
      )
    }

    @Test
    fun `passes null email in body when no Nomis email available`() {
      authApiService.syncEmailWithNomis("user1", null)

      verify(userWebClientUtils).postWithBody(
        mapOf("email" to null),
        "/api/prisonuser/{username}/email/sync",
        "user1",
      )
    }
  }

  @Nested
  inner class UpdateEmail {
    @Test
    fun `passes correct URI, email body and username to userWebClientUtils`() {
      authApiService.updateEmail("user1", "updated@example.com")

      verify(userWebClientUtils).putWithBody(
        mapOf("email" to "updated@example.com"),
        "/api/prisonuser/{username}/email",
        "user1",
      )
    }
  }

  @Nested
  inner class FindUserEmails {
    @Test
    fun `passes correct URI and username list body to webclient`() {
      val usernames = listOf("user1", "user2")
      whenever(
        serviceWebClientUtils.postWithResponse("/api/prisonuser/email", usernames, EmailList::class.java),
      ).thenReturn(EmailList())

      authApiService.findUserEmails(usernames)

      verify(serviceWebClientUtils).postWithResponse("/api/prisonuser/email", usernames, EmailList::class.java)
    }
  }

  @Nested
  inner class FindUsers {
    @Test
    fun `passes all non-null params as query parameters to userWebClientUtils`() {
      authApiService.findUsers(
        name = "john",
        status = Status.ACTIVE,
        authSources = listOf(AuthSource.auth),
        page = 0,
        size = 10,
        sort = "lastName,asc",
      )

      verify(userWebClientUtils).getWithParams<Any>(
        eq("/api/user/search"),
        any<ParameterizedTypeReference<Any>>(),
        eq(
          mapOf(
            "name" to "john",
            "status" to Status.ACTIVE,
            "authSources" to listOf(AuthSource.auth),
            "page" to 0,
            "size" to 10,
            "sort" to "lastName,asc",
          ),
        ),
      )
    }

    @Test
    fun `omits null params from the query`() {
      authApiService.findUsers(
        name = null,
        status = null,
        authSources = null,
        page = 0,
        size = 10,
        sort = null,
      )

      verify(userWebClientUtils).getWithParams<Any>(
        eq("/api/user/search"),
        any<ParameterizedTypeReference<Any>>(),
        eq(mapOf("page" to 0, "size" to 10)),
      )
    }
  }

  @Nested
  inner class AddUserToAllowlist {
    @Test
    fun `passes correct URI and request body to userWebClientUtils`() {
      val request = UserAllowlistAddRequest(
        username = "user1",
        email = "user1@example.com",
        firstName = "First",
        lastName = "Last",
        reason = "Access required for support",
        accessPeriod = AccessPeriod.SIX_MONTHS,
      )

      authApiService.addUserToAllowlist(request)

      verify(userWebClientUtils).postWithBody(request, "/api/user/allowlist")
    }
  }

  @Nested
  inner class GetAllAllowlistUsers {
    @Test
    fun `passes name, status, userType and pageable fields to userWebClientUtils`() {
      val pageable = PageRequest.of(1, 20)

      authApiService.getAllAllowlistUsers(
        name = "smith",
        status = AllowListStatus.ACTIVE,
        userType = UserAllowlistUserType.DIGITAL,
        pageable = pageable,
      )

      verify(userWebClientUtils).getWithParams<Any>(
        eq("/api/user/allowlist"),
        any<ParameterizedTypeReference<Any>>(),
        eq(
          mapOf(
            "name" to "smith",
            "status" to AllowListStatus.ACTIVE,
            "userType" to UserAllowlistUserType.DIGITAL,
            "page" to 1,
            "size" to 20,
          ),
        ),
      )
    }

    @Test
    fun `omits null name and userType from query params`() {
      val pageable = PageRequest.of(0, 10)

      authApiService.getAllAllowlistUsers(
        name = null,
        status = AllowListStatus.ACTIVE,
        userType = null,
        pageable = pageable,
      )

      verify(userWebClientUtils).getWithParams<Any>(
        eq("/api/user/allowlist"),
        any<ParameterizedTypeReference<Any>>(),
        eq(
          mapOf(
            "status" to AllowListStatus.ACTIVE,
            "page" to 0,
            "size" to 10,
          ),
        ),
      )
    }
  }

  @Nested
  inner class GetAllowlistUser {
    @Test
    fun `passes correct URI and username to userWebClientUtils`() {
      authApiService.getAllowlistUser("user1")

      verify(userWebClientUtils).get(
        "/api/user/allowlist/{username}",
        UserAllowlistDetail::class.java,
        "user1",
      )
    }
  }

  @Nested
  inner class UpdateAllowlistUserAccess {
    @Test
    fun `passes correct URI, id and patch request body to userWebClientUtils`() {
      val id = UUID.randomUUID()
      val patchRequest = UserAllowlistPatchRequest(
        reason = "Updated reason",
        accessPeriod = AccessPeriod.TWELVE_MONTHS,
      )

      authApiService.updateAllowlistUserAccess(id, patchRequest)

      verify(userWebClientUtils).patchWithBody(patchRequest, "/api/user/allowlist/{id}", id)
    }
  }
}

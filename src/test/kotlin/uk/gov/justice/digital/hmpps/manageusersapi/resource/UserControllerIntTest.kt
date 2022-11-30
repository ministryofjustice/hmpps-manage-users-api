package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageusersapi.service.NomisUserDetails

class UserControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class CreateUser {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/users")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf()))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_ADM"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_ADM"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong scope`() {
      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_ROLE"), scopes = listOf("read")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_ADM"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create Central Admin user`() {
      nomisApiMockServer.stubCreateCentralAdminUser()
      hmppsAuthMockServer.stubForNewToken()
      externalUsersApiMockServer.stubValidEmailDomain()

      val nomisUserDetails = webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_ADM"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(NomisUserDetails::class.java)
        .returnResult().responseBody!!

      assertThat(nomisUserDetails.username).isEqualTo("TEST1")
      assertThat(nomisUserDetails.firstName).isEqualTo("Test")
      assertThat(nomisUserDetails.lastName).isEqualTo("User")
      assertThat(nomisUserDetails.primaryEmail).isEqualTo("test@test.com")

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/admin-account"))
          .withRequestBody(
            containing(
              """
              {"username":"TEST1","email":"test@gov.uk","firstName":"Test","lastName":"User"}
              """.trimIndent()
            )
          )
      )
    }

    @Test
    fun `create General user`() {
      nomisApiMockServer.stubCreateGeneralUser()
      hmppsAuthMockServer.stubForNewToken()

      val nomisUserDetails = webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_GEN",
              "defaultCaseloadId" to "MDI"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(NomisUserDetails::class.java)
        .returnResult().responseBody!!

      assertThat(nomisUserDetails.username).isEqualTo("TEST1")
      assertThat(nomisUserDetails.firstName).isEqualTo("Test")
      assertThat(nomisUserDetails.lastName).isEqualTo("User")
      assertThat(nomisUserDetails.primaryEmail).isEqualTo("test@test.com")

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/general-account"))
          .withRequestBody(
            containing(
              """
              {"username":"TEST1","email":"test@gov.uk","firstName":"Test","lastName":"User","defaultCaseloadId":"MDI"}
              """.trimIndent()
            )
          )
      )
    }

    @Test
    fun `create Local admin user`() {
      nomisApiMockServer.stubCreateLocalAdminUser()
      hmppsAuthMockServer.stubForNewToken()

      val nomisUserDetails = webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_LSA",
              "defaultCaseloadId" to "MDI"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(NomisUserDetails::class.java)
        .returnResult().responseBody!!

      assertThat(nomisUserDetails.username).isEqualTo("TEST1")
      assertThat(nomisUserDetails.firstName).isEqualTo("Test")
      assertThat(nomisUserDetails.lastName).isEqualTo("User")
      assertThat(nomisUserDetails.primaryEmail).isEqualTo("test@test.com")

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/local-admin-account"))
          .withRequestBody(
            containing(
              """
              {"username":"TEST1","email":"test@gov.uk","firstName":"Test","lastName":"User","localAdminGroup":"MDI"}
              """.trimIndent()
            )
          )
      )
    }
  }

  @Nested
  inner class CreateUserError {
    @Test
    fun `create central admin user returns error when username already exists`() {
      nomisApiMockServer.stubCreateCentralAdminUserConflict()
      hmppsAuthMockServer.stubForNewToken()
      externalUsersApiMockServer.stubValidEmailDomain()

      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_ADM"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["errorCode"] as Int).isEqualTo(601)
          assertThat(it["userMessage"] as String).isEqualTo("Unable to create user: TEST1 with reason: username already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("Unable to create user: TEST1 with reason: username already exists")
        }
    }

    @Test
    fun `create central admin user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateCentralAdminUserWithErrorFail(BAD_REQUEST)

      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "DUD_FIRST_NAME",
              "lastName" to "User",
              "userType" to "DPS_ADM"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(BAD_REQUEST)
        .expectBody()
        .jsonPath("status").isEqualTo("400")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Validation failure: First name must consist of alphabetical characters only and a max 35 chars")
          assertThat(it["developerMessage"] as String).isEqualTo("A bigger message")
        }
    }

    @Test
    fun `create general user returns error when username already exists`() {
      nomisApiMockServer.stubCreateGeneralUserConflict()
      externalUsersApiMockServer.stubValidEmailDomain()

      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_GEN",
              "defaultCaseloadId" to "MDI"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["errorCode"] as Int).isEqualTo(601)
          assertThat(it["userMessage"] as String).isEqualTo("Unable to create user: TEST1 with reason: username already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("Unable to create user: TEST1 with reason: username already exists")
        }
    }

    @Test
    fun `create general user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateGeneralUserWithErrorFail(BAD_REQUEST)

      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "DUD_FIRST_NAME",
              "lastName" to "User",
              "userType" to "DPS_GEN",
              "defaultCaseloadId" to "MDI"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(BAD_REQUEST)
        .expectBody()
        .jsonPath("status").isEqualTo("400")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Validation failure: First name must consist of alphabetical characters only and a max 35 chars")
          assertThat(it["developerMessage"] as String).isEqualTo("A bigger message")
        }
    }

    @Test
    fun `create local admin user returns error when username already exists`() {
      nomisApiMockServer.stubCreateLocalAdminUserConflict()
      externalUsersApiMockServer.stubValidEmailDomain()

      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_LSA",
              "defaultCaseloadId" to "MDI"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Unable to create user: TEST1 with reason: username already exists")
          assertThat(it["errorCode"] as Int).isEqualTo(601)
          assertThat(it["developerMessage"] as String).isEqualTo("Unable to create user: TEST1 with reason: username already exists")
        }
    }

    @Test
    fun `create local admin user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateLocalAdminUserWithErrorFail(BAD_REQUEST)

      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "DUD_FIRST_NAME",
              "lastName" to "User",
              "userType" to "DPS_LSA",
              "defaultCaseloadId" to "MDI"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(BAD_REQUEST)
        .expectBody()
        .jsonPath("status").isEqualTo("400")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Validation failure: First name must consist of alphabetical characters only and a max 35 chars")
          assertThat(it["developerMessage"] as String).isEqualTo("A bigger message")
        }
    }

    @Test
    fun `create local admin user returns error for invalid email domain `() {
      nomisApiMockServer.stubCreateLocalAdminUserConflict()
      externalUsersApiMockServer.stubInvalidEmailDomain()

      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@invaliddomain.com",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_LSA",
              "defaultCaseloadId" to "MDI"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["errorCode"] as Int).isEqualTo(602)
          assertThat(it["userMessage"] as String).isEqualTo("Invalid Email domain: invaliddomain.com with reason: Email domain not valid")
          assertThat(it["developerMessage"] as String).isEqualTo("Invalid Email domain: invaliddomain.com with reason: Email domain not valid")
        }
    }
  }

  @Nested
  inner class CreateUserMissingFieldValidation {
    @Test
    fun `create user returns error when user type does not exist`() {
      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User"
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  inner class SendEnableUserEmail {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/users/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/users/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.put().uri("/users/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun ` should fail with not_found for invalid user id`() {
      externalUsersApiMockServer.stubPutEnableInvalidUser("2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f")
      webTestClient.put().uri("/users/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(it["userMessage"] as String)
            .startsWith("User not found: User 2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f not found")
          assertThat(it["developerMessage"] as String)
            .startsWith("User 2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f not found")
        }
    }

    @Test
    fun `should fail with forbidden  for user not in group`() {
      externalUsersApiMockServer.stubPutEnableFailUserNotInGroup()
      webTestClient.put().uri("/users/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(HttpStatus.FORBIDDEN.value())
          assertThat(it["userMessage"] as String)
            .startsWith("User group relationship exception: Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups")
          assertThat(it["developerMessage"] as String)
            .startsWith("Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups")
        }
    }

    @Test
    fun enableUser() {
      externalUsersApiMockServer.stubPutEnableUser("2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f")
      webTestClient.put().uri("/users/2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNoContent
    }
  }
}

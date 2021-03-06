package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
      hmppsAuthMockServer.stubForValidEmailDomain()

      val nomisUserDetails = webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER", "ROLE_CREATE_EMAIL_TOKEN")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER", "ROLE_CREATE_EMAIL_TOKEN")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER", "ROLE_CREATE_EMAIL_TOKEN")))
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
      hmppsAuthMockServer.stubForValidEmailDomain()

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
          assertThat(it["errorCode"] as Integer).isEqualTo(601)
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
      hmppsAuthMockServer.stubForValidEmailDomain()

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
          assertThat(it["errorCode"] as Integer).isEqualTo(601)
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
      hmppsAuthMockServer.stubForValidEmailDomain()

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
          assertThat(it["errorCode"] as Integer).isEqualTo(601)
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
      hmppsAuthMockServer.stubForInValidEmailDomain()

      webTestClient.post().uri("/users")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@1gov.uk",
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
          assertThat(it["errorCode"] as Integer).isEqualTo(602)
          assertThat(it["userMessage"] as String).isEqualTo("Invalid Email domain: 1gov.uk with reason: Email domain not valid")
          assertThat(it["developerMessage"] as String).isEqualTo("Invalid Email domain: 1gov.uk with reason: Email domain not valid")
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
              "lastName" to "User",
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
    }
  }
}

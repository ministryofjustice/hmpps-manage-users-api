package uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis

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
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.NomisUserCreatedDetails

class UserControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class CreateUser {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/prisonusers")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/prisonusers")
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
      webTestClient.post().uri("/prisonusers")
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
      webTestClient.post().uri("/prisonusers")
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

      val nomisUserDetails = webTestClient.post().uri("/prisonusers")
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
        .expectBody(NomisUserCreatedDetails::class.java)
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

      val nomisUserDetails = webTestClient.post().uri("/prisonusers")
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
        .expectBody(NomisUserCreatedDetails::class.java)
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

      val nomisUserDetails = webTestClient.post().uri("/prisonusers")
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
        .expectBody(NomisUserCreatedDetails::class.java)
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

      webTestClient.post().uri("/prisonusers")
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
          assertThat(it["userMessage"] as String).isEqualTo("User already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("User TEST21 already exists")
        }
    }

    @Test
    fun `create central admin user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateCentralAdminUserWithErrorFail(BAD_REQUEST)

      webTestClient.post().uri("/prisonusers")
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

      webTestClient.post().uri("/prisonusers")
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
          assertThat(it["userMessage"] as String).isEqualTo("User already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("User TEST21 already exists")
        }
    }

    @Test
    fun `create general user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateGeneralUserWithErrorFail(BAD_REQUEST)

      webTestClient.post().uri("/prisonusers")
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

      webTestClient.post().uri("/prisonusers")
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
          assertThat(it["userMessage"] as String).isEqualTo("User already exists")
          assertThat(it["errorCode"] as Int).isEqualTo(601)
          assertThat(it["developerMessage"] as String).isEqualTo("User TEST21 already exists")
        }
    }

    @Test
    fun `create local admin user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateLocalAdminUserWithErrorFail(BAD_REQUEST)

      webTestClient.post().uri("/prisonusers")
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

      webTestClient.post().uri("/prisonusers")
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
      webTestClient.post().uri("/prisonusers")
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
  inner class FindUsersByFirstAndLastName {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/prisonusers?firstName=First&lastName=Last")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun ` prison user found success`() {
      nomisApiMockServer.stubFindUsersByFirstAndLastName("First", "Last")
      hmppsAuthMockServer.stubUserEmails()
      webTestClient.get().uri("/prisonusers?firstName=First&lastName=Last")
        .headers(setAuthorisation(roles = listOf("ROLE_USE_OF_FORCE")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("[*].username").value<List<String>> {
          assertThat(it).containsExactlyElementsOf(listOf("NUSER_GEN", "NUSER_ADM"))
        }
        .jsonPath("$.[0]").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "username" to "NUSER_GEN",
              "staffId" to 123456,
              "email" to "First.Last@digital.justice.gov.uk",
              "verified" to true,
              "firstName" to "First",
              "lastName" to "Last",
              "name" to "First Last",
              "activeCaseLoadId" to "MDI",
            )
          )
        }
    }

    @Test
    fun `Prison user not found`() {
      nomisApiMockServer.stubGetWithEmptyReturn("/users/staff?firstName=not&lastName=found", HttpStatus.OK)

      webTestClient
        .get().uri("/prisonusers?firstName=not&lastName=found")
        .headers(setAuthorisation(roles = listOf("ROLE_STAFF_SEARCH")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
    }
  }
}

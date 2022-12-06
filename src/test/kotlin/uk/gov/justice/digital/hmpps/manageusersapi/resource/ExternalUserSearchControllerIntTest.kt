package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class ExternalUserSearchControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class FindUsersByEmail {

    @Test
    fun `access forbidden when unauthorised`() {
      webTestClient.get().uri("/externalusers")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should respond with no content when email address null`() {
      webTestClient.get().uri("/externalusers")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `should respond with no content when external users api responds with no content`() {
      val email = "testy@testing.co.uk"
      externalUsersApiMockServer.stubNoUsersFound(email)

      webTestClient.get().uri("/externalusers?email=$email")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `should respond with user data returned from external users api`() {
      val email = "auth_test2@digital.justice.gov.uk"
      externalUsersApiMockServer.stubUsersByEmail(email)

      webTestClient.get().uri("/externalusers?email=$email")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].userId").value<List<String>> { assertThat(it).hasSize(2) }
        .jsonPath("$[1].userId").isEqualTo("9e84f1e4-59c8-4b10-927a-9cf9e9a30791")
        .jsonPath("$[1].username").isEqualTo("AUTH_EXPIRED")
        .jsonPath("$[1].email").isEqualTo("auth_test2@digital.justice.gov.uk")
        .jsonPath("$[1].firstName").isEqualTo("Auth")
        .jsonPath("$[1].lastName").isEqualTo("Expired")
        .jsonPath("$[1].locked").isEqualTo(false)
        .jsonPath("$[1].enabled").isEqualTo(true)
        .jsonPath("$[1].verified").isEqualTo(true)
        .jsonPath("$[1].lastLoggedIn").isNotEmpty
        .jsonPath("$[1].inactiveReason").isEqualTo("Expired")
    }
  }

  @Nested
  inner class FindUsersByUserName {

    @Test
    fun `access forbidden when unauthorised`() {
      webTestClient.get().uri("/externalusers/users")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should respond with not found when user not found`() {
      webTestClient.get().uri("/externalusers/users")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `should respond with no content when external users api responds with no content`() {
      val email = "testy@testing.co.uk"
      externalUsersApiMockServer.stubNoUsersFound(email)

      webTestClient.get().uri("/externalusers?email=$email")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `should respond with user data returned from external users api`() {
      val userName = "AUTH_ADM"
      externalUsersApiMockServer.stubUsersByUserName(userName)

      webTestClient.get().uri("/externalusers/$userName")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.userId").isEqualTo("5105a589-75b3-4ca0-9433-b96228c1c8f3")
        .jsonPath("$.username").isEqualTo("AUTH_ADM")
        .jsonPath("$.email").isEqualTo("auth_test2@digital.justice.gov.uk")
        .jsonPath("$.firstName").isEqualTo("Auth")
        .jsonPath("$.lastName").isEqualTo("Adm")
        .jsonPath("$.locked").isEqualTo(false)
        .jsonPath("$.enabled").isEqualTo(true)
        .jsonPath("$.verified").isEqualTo(true)
        .jsonPath("$.lastLoggedIn").isNotEmpty
        .jsonPath("$.inactiveReason").isEqualTo("Expired")
    }
  }
  @Test
  fun `should respond with NOT_FOUND from external users api`() {
    val userName = "AUTH_ADM"
    externalUsersApiMockServer.stubNoUsersFoundForUserName(userName)

    webTestClient.get().uri("/externalusers/$userName")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isNotFound
  }
}

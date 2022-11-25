package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class EmailNotificationControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class SendEnableEmail {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/notify/enable-user")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/notify/enable-user")
        .headers(setAuthorisation(roles = listOf()))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "username" to "test_user",
              "firstName" to "firstName",
              "lastName" to "lastName",
              "admin" to "admin",
              "email" to "email@gov.uk"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/notify/enable-user")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "username" to "test_user",
              "firstName" to "firstName",
              "lastName" to "lastName",
              "admin" to "admin",
              "email" to "email@gov.uk"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun sendEnableEmail() {
      externalUsersApiMockServer.stubPostSendEnableEmail()
      webTestClient.post().uri("/notify/enable-user")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "username" to "test_user",
              "firstName" to "firstName",
              "admin" to "admin",
              "email" to "email@gov.uk"
            )
          )
        )
        .exchange()
        .expectStatus().isOk
    }
  }
}

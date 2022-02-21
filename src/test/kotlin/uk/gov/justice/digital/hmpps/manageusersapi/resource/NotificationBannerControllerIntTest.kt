package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class NotificationBannerControllerIntTest : IntegrationTestBase() {

  @Test
  fun `get notification message - unauthorized when no authority`() {

    webTestClient.get().uri("/notification/banner/test")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get notification message`() {

    webTestClient.get().uri("/notification/banner/TEST")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.message").isEqualTo("Test banner message\n")
  }

  @Test
  fun `get notification message - null message`() {
    webTestClient.get().uri("/notification/banner/EMPTY")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.message").isEmpty
  }

  @Test
  fun `get notification message - null message file does not exist`() {
    webTestClient.get().uri("/notification/banner/NOFILE")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.message").isEmpty
  }

  @Test
  fun `get notification message - bad request if NotificationType enum does not exist`() {
    webTestClient.get().uri("/notification/banner/none")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isBadRequest
  }
}

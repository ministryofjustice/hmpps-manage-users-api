package uk.gov.justice.digital.hmpps.manageusersapi.integration.health

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class HealthCheckTest : IntegrationTestBase() {

  @Test
  fun `Health page reports ok`() {
    stubPingWithResponse(200)
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
      .jsonPath("components.authHealthCheck.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.deliusApiHealthCheck.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.externalUsersApiHealthCheck.details.HttpStatus").isEqualTo("OK")
      .jsonPath("components.nomisApiHealthCheck.details.HttpStatus").isEqualTo("OK")
  }

  @Test
  fun `Health page reports down`() {
    stubPingWithResponse(503)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .is5xxServerError
      .expectBody()
      .jsonPath("status").isEqualTo("DOWN")
      .jsonPath("components.authHealthCheck.status").isEqualTo("DOWN")
  }

  @Test
  fun `Health ping page is accessible`() {
    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  private fun stubPingWithResponse(status: Int) {
    hmppsAuthMockServer.stubHealthPing(status)
    deliusApiMockServer.stubHealthPing(status)
    nomisApiMockServer.stubHealthPing(status)
    externalUsersApiMockServer.stubHealthPing(status)
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class UserCaseloadsControllerIntTest : IntegrationTestBase() {
  @Nested
  inner class GetCaseloads {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/prisonusers/bob/caseloads")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/prisonusers/bob/caseloads")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri("/prisonusers/bob/caseloads")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun ` get user caseloads`() {
      nomisApiMockServer.stubGetUserCaseloads("bob")

      webTestClient.get().uri("/prisonusers/bob/caseloads")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.username").isEqualTo("bob")
        .jsonPath("$.caseloads[0].name").isEqualTo("WANDSWORTH (HMP)")

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo("/users/bob/caseloads")),
      )
    }
  }

  @Nested
  inner class AddCaseloads {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/prisonusers/bob/caseloads")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/prisonusers/bob/caseloads")
        .headers(setAuthorisation(roles = listOf()))
        .body(fromValue(listOf("CASELOAD1", "CASELOAD2")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/prisonusers/bob/caseloads")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .body(fromValue(listOf("CASELOAD1", "CASELOAD2")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `post user caseloads calls nomis api`() {
      nomisApiMockServer.stubPostUserCaseloads("bob", "[\"CASELOAD1\",\"CASELOAD2\"]")

      webTestClient.post().uri("/prisonusers/bob/caseloads")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .body(fromValue(listOf("CASELOAD1", "CASELOAD2")))
        .exchange()
        .expectStatus().isCreated

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/bob/caseloads"))
          .withRequestBody(WireMock.equalToJson("[\"CASELOAD1\",\"CASELOAD2\"]")),
      )
    }
  }

  @Nested
  inner class RemoveCaseload {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/prisonusers/bob/caseloads/LEI")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/prisonusers/bob/caseloads/LEI")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.delete().uri("/prisonusers/bob/caseloads/LEI")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `remove user caseload calls nomis api`() {
      nomisApiMockServer.stubDeleteUserCaseloads("bob", "LEI")

      webTestClient.delete().uri("/prisonusers/bob/caseloads/LEI")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(WireMock.deleteRequestedFor(urlEqualTo("/users/bob/caseloads/LEI")))
    }
  }
}

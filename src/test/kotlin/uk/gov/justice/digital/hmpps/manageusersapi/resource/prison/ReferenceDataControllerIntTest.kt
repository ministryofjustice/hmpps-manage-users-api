package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class ReferenceDataControllerIntTest : IntegrationTestBase() {
  @Nested
  inner class GetCaseloads {

    @Test
    fun `get caseloads when no authority`() {
      webTestClient.get().uri("/prisonusers/reference-data/caseloads")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun ` get user roles - long role name added from hmpps-external-users `() {
      nomisApiMockServer.stubGetCaseloads("/reference-data/caseloads")

      webTestClient.get().uri("/prisonusers/reference-data/caseloads")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isOk
        .expectBody()

      nomisApiMockServer.verify(getRequestedFor(urlEqualTo("/reference-data/caseloads")))
    }
  }
}

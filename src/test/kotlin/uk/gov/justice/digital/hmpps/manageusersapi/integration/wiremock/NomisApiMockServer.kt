package uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.http.HttpStatus
import wiremock.org.apache.http.client.methods.RequestBuilder.post

class NomisApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8093
  }

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status)
      )
    )
  }

  fun stubCreateRole() {
    stubFor(
      WireMock.post(urlEqualTo("/roles"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
        )
    )
  }

  fun stubPutRole(roleCode: String) {
    stubFor(
      put("/roles/$roleCode")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }
}

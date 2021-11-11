package uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
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
      post(urlEqualTo("/roles"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
        )
    )
  }

  fun stubCreateRoleFail(status: HttpStatus) {
    stubFor(
      post(urlEqualTo("/roles"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
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

  fun stubPutRoleFail(roleCode: String, status: HttpStatus) {
    stubFor(
      put("/roles/$roleCode")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
        )
    )
  }

  fun stubGetUserRoles(username: String) {
    stubFor(
      get("/users/$username/roles")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                    "username": "BOB",
                    "active": true,
                    "accountType": "ADMIN",
                    "activeCaseload": {
                    "id": "CADM_I",
                    "name": "Central Administration Caseload For Hmps"
                    },
                     "dpsRoles": [
                      {
                        "code": "AUDIT_VIEWER",
                        "name": "Audit viewer",
                        "sequence": 1,
                        "type": "APP",
                        "adminRoleOnly": true
                      },
                      {
                        "code": "AUTH_GROUP_MANAGER",
                        "name": "Auth Group Manager that has mo",
                        "sequence": 1,
                        "type": "APP",
                        "adminRoleOnly": true
                      }
                      ]
                }
              """.trimIndent()
            )
        )
    )
  }
}

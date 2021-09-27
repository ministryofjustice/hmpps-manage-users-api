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

class HmppsAuthMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8090
  }

  fun stubGrantToken() {
    stubFor(
      post(urlEqualTo("/auth/oauth/token"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                    "token_type": "bearer",
                    "access_token": "ABCDE"
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/auth/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status)
      )
    )
  }

  fun stubGetRolesDetails() {
    stubFor(
      get(urlEqualTo("/auth/api/roles/AUTH_GROUP_MANAGER"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                    "roleCode": "AUTH_GROUP_MANAGER",
                    "roleName": "Group Manager",
                    "roleDescription": "Allow Group Manager to administer the account within their groups",
                    "adminType": [
                        {
                            "adminTypeCode": "EXT_ADM",
                            "adminTypeName": "External Administrator"
                        }
                    ]
                  }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateRole() {
    stubFor(
      post(urlEqualTo("/auth/api/roles"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
        )
    )
  }

  fun stubCreateRoleFail(status: HttpStatus) {
    stubFor(
      post(urlEqualTo("/auth/api/roles"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
        )
    )
  }

  fun stubPutRoleName(roleCode: String) {
    stubFor(
      put("/auth/api/roles/$roleCode")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutRoleNameFail(roleCode: String, status: HttpStatus) {
    stubFor(
      put("/auth/api/roles/$roleCode")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
        )
    )
  }

  fun stubPutRoleDescription(roleCode: String) {
    stubFor(
      put("/auth/api/roles/$roleCode/description")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutRoleDescriptionFail(roleCode: String, status: HttpStatus) {
    stubFor(
      put("/auth/api/roles/$roleCode/description")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
        )
    )
  }

  fun stubPutRoleAdminType(roleCode: String) {
    stubFor(
      put("/auth/api/roles/$roleCode/admintype")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutRoleAdminTypeFail(roleCode: String, status: HttpStatus) {
    stubFor(
      put("/auth/api/roles/$roleCode/admintype")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
        )
    )
  }
}

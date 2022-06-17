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

  fun stubCreateCentralAdminUser() {
    stubFor(
      post(urlEqualTo("/users/admin-account"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
            .withBody(
              """
              { 
                "username": "TEST1",
                "firstName": "Test",
                "lastName": "User",
                "primaryEmail": "test@test.com",
                "activeCaseloadId" : "CADM_I",
                "accountType": "ADMIN"
              }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateGeneralUser() {
    stubFor(
      post(urlEqualTo("/users/general-account"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
            .withBody(
              """
              { 
                "username": "TEST1",
                "firstName": "Test",
                "lastName": "User",
                "primaryEmail": "test@test.com",
                "activeCaseloadId": "MDI",
                "accountType": "GENERAL"
              }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateLocalAdminUser() {
    stubFor(
      post(urlEqualTo("/users/local-admin-account"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
            .withBody(
              """
              { 
                "username": "TEST1",
                "firstName": "Test",
                "lastName": "User",
                "primaryEmail": "test@test.com",
                "activeCaseloadId": "MDI",
                "accountType": "ADMIN"
              }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateCentralAdminUserConflict() {
    stubFor(
      post(urlEqualTo("/users/admin-account"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.CONFLICT.value())
            .withBody(
              """{
                "status": ${HttpStatus.CONFLICT.value()},
                "errorCode": null,
                "userMessage": "User already exists",
                "developerMessage": "User TEST21 already exists"
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateCentralAdminUserWithErrorFail(status: HttpStatus) {
    stubFor(
      post(urlEqualTo("/users/admin-account"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "Validation failure: First name must consist of alphabetical characters only and a max 35 chars",
                "developerMessage": "A bigger message"
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateGeneralUserConflict() {
    stubFor(
      post(urlEqualTo("/users/general-account"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.CONFLICT.value())
            .withBody(
              """{
                "status": ${HttpStatus.CONFLICT.value()},
                "errorCode": null,
                "userMessage": "User already exists",
                "developerMessage": "User TEST21 already exists"
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateGeneralUserWithErrorFail(status: HttpStatus) {
    stubFor(
      post(urlEqualTo("/users/general-account"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "Validation failure: First name must consist of alphabetical characters only and a max 35 chars",
                "developerMessage": "A bigger message"
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateLocalAdminUserConflict() {
    stubFor(
      post(urlEqualTo("/users/local-admin-account"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.CONFLICT.value())
            .withBody(
              """{
                "status": ${HttpStatus.CONFLICT.value()},
                "errorCode": null,
                "userMessage": "User already exists",
                "developerMessage": "User TEST21 already exists"
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateLocalAdminUserWithErrorFail(status: HttpStatus) {
    stubFor(
      post(urlEqualTo("/users/local-admin-account"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "Validation failure: First name must consist of alphabetical characters only and a max 35 chars",
                "developerMessage": "A bigger message"
               }
              """.trimIndent()
            )
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

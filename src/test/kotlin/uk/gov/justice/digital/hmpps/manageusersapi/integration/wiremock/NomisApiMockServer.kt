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
          .withStatus(status),
      ),
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
              """.trimIndent(),
            ),
        ),
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
              """.trimIndent(),
            ),
        ),
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
              """.trimIndent(),
            ),
        ),
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
                "errorCode": 601,
                "userMessage": "User already exists",
                "developerMessage": "User TEST21 already exists"
               }
              """.trimIndent(),
            ),
        ),
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
              """.trimIndent(),
            ),
        ),
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
                "errorCode": 601,
                "userMessage": "User already exists",
                "developerMessage": "User TEST21 already exists"
               }
              """.trimIndent(),
            ),
        ),
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
              """.trimIndent(),
            ),
        ),
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
                "errorCode": 601,
                "userMessage": "User already exists",
                "developerMessage": "User TEST21 already exists"
               }
              """.trimIndent(),
            ),
        ),
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
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubCreateRole() {
    stubFor(
      post(urlEqualTo("/roles"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201),
        ),
    )
  }

  fun stubCreateRoleFail(status: HttpStatus, roleCode: String) {
    stubFor(
      post(urlEqualTo("/roles"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": 601,
                "userMessage": "Role already exists: Role with code $roleCode already exists",
                "developerMessage": "Role with code $roleCode already exists"
               }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubPostFailEmptyResponse(url: String, status: HttpStatus) {
    stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value()),
        ),
    )
  }

  fun stubPutRole(roleCode: String) {
    stubFor(
      put("/roles/$roleCode")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json"))),
        ),
    )
  }

  fun stubPutRoleFail(roleCode: String, status: HttpStatus) {
    stubFor(
      put("/roles/$roleCode")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value()),
        ),
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
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubFindUserByUsername(username: String) {
    stubFor(
      get("/users/$username")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """ {
                "username": "NUSER_GEN",
                "staffId": 123456,
                "firstName": "Nomis",
                "lastName": "Take",
                "activeCaseloadId": "MDI",
                "accountStatus": "OPEN",
                "accountType": "GENERAL",
                "primaryEmail": "nomis.usergen@digital.justice.gov.uk",
                "dpsRoleCodes": [
                  "MAINTAIN_ACCESS_ROLES",
                  "GLOBAL_SEARCH",
                  "HMPPS_REGISTERS_MAINTAINER",
                  "HPA_USER"
                ],
                "accountNonLocked": true,
                "credentialsNonExpired": false,
                "enabled": true,
                "admin": false,
                "active": true,
                "staffStatus": "ACTIVE"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubFindUsersByFirstAndLastName(firstName: String, lastName: String) {
    stubFor(
      get("/users/staff?firstName=$firstName&lastName=$lastName")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """ 
                [
                  {
                    "username": "NUSER_GEN",
                    "staffId": 123456,
                    "firstName": "$firstName",
                    "lastName": "$lastName",
                    "active": true,
                    "status": "OPEN",
                    "locked": false,
                    "expired": false,
                    "activeCaseload": {
                        "id": "MDI",
                        "name": "Moorland (HMP & YOI)"
                    },
                    "dpsRoleCount": 4,
                    "email": "$firstName.$lastName@digital.justice.gov.uk",
                    "staffStatus": "ACTIVE"
                },
                {
                    "username": "NUSER_ADM",
                    "staffId": 456789,
                    "firstName": "$firstName",
                    "lastName": "$lastName",
                    "active": true,
                    "status": "OPEN",
                    "locked": false,
                    "expired": false,
                    "activeCaseload": {
                        "id": "CADM_I",
                        "name": "Central Administration Caseload For Hmps"
                    },
                    "dpsRoleCount": 11,
                    "email": "$firstName.$lastName@digital.justice2.gov.uk",
                    "staffStatus": "ACTIVE"
                }
            ]
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubGetWithEmptyReturn(url: String, status: HttpStatus) {
    stubFor(
      get(url)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody("[]"),
        ),
    )
  }

  fun stubGetFail(url: String, status: HttpStatus) {
    stubFor(
      get(url)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "Nomis User message for GET failed",
                "developerMessage": "Developer Nomis user message for GET failed"
              }
              """.trimIndent(),
            ),
        ),
    )
  }
}

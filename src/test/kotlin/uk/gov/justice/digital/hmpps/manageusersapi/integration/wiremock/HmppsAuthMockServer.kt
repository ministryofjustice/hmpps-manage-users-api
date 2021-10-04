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

  fun stubGetAllRoles() {
    stubFor(
      get(urlEqualTo("/auth/api/roles?page=0&size=10&sort=roleName,asc"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                      "content": [
                          {
                              "roleCode": "AUDIT_VIEWER",
                              "roleName": "Audit viewer"
                          },
                          {
                              "roleCode": "AUTH_GROUP_MANAGER",
                              "roleName": "Auth Group Manager"
                          },
                          {
                              "roleCode": "ROLE_1",
                              "roleName": "role 1"
                          },
                          {
                              "roleCode": "ROLE_2",
                              "roleName": "role 2"
                          },
                          {
                              "roleCode": "ROLE_3",
                              "roleName": "role 3"
                          },
                          {
                              "roleCode": "ROLE_5",
                              "roleName": "role 5"
                          },
                          {
                              "roleCode": "ROLE_6",
                              "roleName": "role 6"
                          },
                          {
                              "roleCode": "ROLE_7",
                              "roleName": "role 7"
                          },
                          {
                              "roleCode": "ROLE_8",
                              "roleName": "role 8"
                          }
                      ],
                      "pageable": {
                          "sort": {
                              "sorted": true,
                              "unsorted": false,
                              "empty": false
                          },
                          "offset": 0,
                          "pageNumber": 0,
                          "pageSize": 10,
                          "paged": true,
                          "unpaged": false
                      },
                      "last": false,
                      "totalPages": 4,
                      "totalElements": 37,
                      "size": 10,
                      "number": 0,
                      "sort": {
                          "sorted": true,
                          "unsorted": false,
                          "empty": false
                      },
                      "numberOfElements": 10,
                      "first": true,
                      "empty": false
                  }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetAllRolesPage3Descending() {
    stubFor(
      get(urlEqualTo("/auth/api/roles?page=3&size=4&sort=roleName,desc"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                      "content": [
                          {
                              "roleCode": "ROLE_8",
                              "roleName": "role 8"
                          },
                          {
                              "roleCode": "ROLE_7",
                              "roleName": "role 7"
                          },
                          {
                              "roleCode": "ROLE_6",
                              "roleName": "role 6"
                          },
                          {
                              "roleCode": "ROLE_5",
                              "roleName": "role 5"
                          }
                      ],
                      "pageable": {
                          "sort": {
                              "sorted": true,
                              "unsorted": false,
                              "empty": false
                          },
                          "offset": 12,
                          "pageNumber": 3,
                          "pageSize": 4,
                          "paged": true,
                          "unpaged": false
                      },
                      "last": false,
                      "totalPages": 10,
                      "totalElements": 37,
                      "size": 4,
                      "number": 3,
                      "sort": {
                          "sorted": true,
                          "unsorted": false,
                          "empty": false
                      },
                      "numberOfElements": 4,
                      "first": false,
                      "empty": false
                  }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetRolesDetails(roleCode: String) {
    stubFor(
      get(urlEqualTo("/auth/api/roles/$roleCode"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                    "roleCode": "$roleCode",
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

  fun stubGetDPSRoleDetails(roleCode: String) {
    stubFor(
      get(urlEqualTo("/auth/api/roles/$roleCode"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                    "roleCode": "$roleCode",
                    "roleName": "Group Manager",
                    "roleDescription": "Allow Group Manager to administer the account within their groups",
                    "adminType": [
                        {
                            "adminTypeCode": "EXT_ADM",
                            "adminTypeName": "External Administrator"
                        },
                        {
                            "adminTypeCode": "DPS_ADM",
                            "adminTypeName": "DPS Central Administrator"
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

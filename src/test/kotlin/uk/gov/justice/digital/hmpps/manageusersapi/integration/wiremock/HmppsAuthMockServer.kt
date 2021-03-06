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

  fun stubGetAllRolesPaged() {
    stubFor(
      get(urlEqualTo("/auth/api/roles/paged?page=0&size=10&sort=roleName,asc&roleName&roleCode&adminTypes"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                      "content": [
                          {
                              "roleCode": "AUDIT_VIEWER",
                              "roleName": "Audit viewer",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "AUTH_GROUP_MANAGER",
                              "roleName": "Auth Group Manager",
                              "roleDescription": "Gives group manager ability to administer user in there groups",
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_1",
                              "roleName": "role 1",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_2",
                              "roleName": "role 2",
                              "roleDescription": "Second role",
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_3",
                              "roleName": "role 3",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_4",
                              "roleName": "role 4",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_5",
                              "roleName": "role 5",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_6",
                              "roleName": "role 6",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_7",
                              "roleName": "role 7",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_8",
                              "roleName": "role 8",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
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

  fun stubGetRoles() {
    stubFor(
      get(urlEqualTo("/auth/api/roles?adminTypes"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
                     [
                          {
                              "roleCode": "AUDIT_VIEWER",
                              "roleName": "Audit viewer",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "AUTH_GROUP_MANAGER",
                              "roleName": "Auth Group Manager",
                              "roleDescription": "Gives group manager ability to administer user in there groups",
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_1",
                              "roleName": "role 1",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_2",
                              "roleName": "role 2",
                              "roleDescription": "Second role",
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_3",
                              "roleName": "role 3",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_4",
                              "roleName": "role 4",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_5",
                              "roleName": "role 5",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_6",
                              "roleName": "role 6",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_7",
                              "roleName": "role 7",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_8",
                              "roleName": "role 8",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          }
                      ]
                  
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetAllRolesPage3Descending() {
    stubFor(
      get(urlEqualTo("/auth/api/roles/paged?page=3&size=4&sort=roleName,desc&roleName&roleCode&adminTypes"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                      "content": [
                          {
                              "roleCode": "ROLE_8",
                              "roleName": "role 8",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_7",
                              "roleName": "role 7",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_6",
                              "roleName": "role 6",
                              "roleDescription": "Sixth role",
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "ROLE_5",
                              "roleName": "role 5",
                              "roleDescription": "Fifth role",
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
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

  fun stubGetAllRolesPagedFilterRoleCode() {
    stubFor(
      get(urlEqualTo("/auth/api/roles/paged?page=0&size=10&sort=roleName,asc&roleName&roleCode=account&adminTypes"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBodyPaged()
            )
        )
    )
  }

  fun stubGetAllRolesPagedFilterRoleName() {
    stubFor(
      get(urlEqualTo("/auth/api/roles/paged?page=0&size=10&sort=roleName,asc&roleName=manager&roleCode&adminTypes"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBodyPaged()
            )
        )
    )
  }

  fun stubGetAllRolesFilterAdminType() {
    stubFor(
      get(urlEqualTo("/auth/api/roles?adminTypes=EXT_ADM"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBody()
            )
        )
    )
  }

  fun stubGetAllRolesFilterAdminTypes() {
    stubFor(
      get(urlEqualTo("/auth/api/roles?adminTypes=EXT_ADM&adminTypes=DPS_ADM"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBody()
            )
        )
    )
  }

  fun stubGetAllRolesPagedFilterAdminType() {
    stubFor(
      get(urlEqualTo("/auth/api/roles/paged?page=0&size=10&sort=roleName,asc&roleName&roleCode&adminTypes=EXT_ADM"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBodyPaged()
            )
        )
    )
  }

  fun stubGetAllRolesPagedFilterAdminTypes() {
    stubFor(
      get(urlEqualTo("/auth/api/roles/paged?page=0&size=10&sort=roleName,asc&roleName&roleCode&adminTypes=EXT_ADM&adminTypes=DPS_ADM"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBodyPaged()
            )
        )
    )
  }

  fun stubGetAllRolesPagedUsingAllFilters() {
    stubFor(
      get(urlEqualTo("/auth/api/roles/paged?page=1&size=10&sort=roleName,asc&roleName=manager&roleCode=account&adminTypes=EXT_ADM&adminTypes=DPS_ADM"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBodyPaged()
            )
        )
    )
  }

  fun apiResponseBody() = """
                      [
                          {
                              "roleCode": "ACCOUNT_MANAGER",
                              "roleName": "The group account manager",
                              "roleDescription": "A group account manager - responsible for managing groups",
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
                      ]
  """.trimIndent()

  fun apiResponseBodyPaged() = """{
                      "content": [
                          {
                              "roleCode": "ACCOUNT_MANAGER",
                              "roleName": "The group account manager",
                              "roleDescription": "A group account manager - responsible for managing groups",
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
                      ],
                      "pageable": {
                          "sort": {
                              "sorted": true,
                              "unsorted": false,
                              "empty": false
                          },
                          "offset": 0,
                          "pageNumber": 1,
                          "pageSize": 10,
                          "paged": true,
                          "unpaged": false
                      },
                      "last": false,
                      "totalPages": 1,
                      "totalElements": 1,
                      "size": 10,
                      "number": 1,
                      "sort": {
                          "sorted": true,
                          "unsorted": false,
                          "empty": false
                      },
                      "numberOfElements": 1,
                      "first": false,
                      "empty": false
                  }
  """.trimIndent()

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

  fun stubGetRoleDetailsFail(status: HttpStatus, roleCode: String) {
    stubFor(
      get(urlEqualTo("/auth/api/roles/$roleCode"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
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

  fun stubGetRolesForRoleName() {
    stubFor(
      get(urlEqualTo("/auth/api/roles?adminTypes=DPS_ADM"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
                     [
                          {
                              "roleCode": "AUDIT_VIEWER",
                              "roleName": "Audit viewer",
                              "roleDescription": null,
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          },
                          {
                              "roleCode": "AUTH_GROUP_MANAGER",
                              "roleName": "Auth Group Manager that has more than 30 characters in the role name",
                              "roleDescription": "Gives group manager ability to administer user in there groups",
                              "adminType": [
                                  {
                                      "adminTypeCode": "EXT_ADM",
                                      "adminTypeName": "External Administrator"
                                  }
                              ]
                          }
                           ]
                  
              """.trimIndent()
            )
        )
    )
  }

  fun stubForNewToken() {
    stubFor(
      post(urlEqualTo("/auth/api/new-token"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "text/plain;charset=UTF-8")))
            .withBody(
              "a25adf13-dbed-4a19-ad07-d1cd95b12500".trimIndent()
            )
        )
    )
  }

  fun stubForValidEmailDomain() {
    stubFor(
      get(urlEqualTo("/auth/api/validate/email-domain?emailDomain=gov.uk"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              "true"
            )
        )
    )
  }

  fun stubForInValidEmailDomain() {
    stubFor(
      get(urlEqualTo("/auth/api/validate/email-domain?emailDomain=1gov.uk"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              "false".trimIndent()
            )
        )
    )
  }
}

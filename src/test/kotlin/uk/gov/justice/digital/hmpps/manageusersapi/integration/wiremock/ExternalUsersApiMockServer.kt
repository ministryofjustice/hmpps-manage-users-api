package uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import java.util.UUID

class ExternalUsersApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8098
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

  fun stubGetAllRolesFilterAdminType() {
    stubFor(
      get(urlEqualTo("/roles?adminTypes=EXT_ADM"))
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
      get(urlEqualTo("/roles?adminTypes=EXT_ADM&adminTypes=DPS_ADM"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBody()
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
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User test message",
                "developerMessage": "Developer test message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateEmailDomainConflict() {
    stubFor(
      post(urlEqualTo("/email-domains"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(CONFLICT.value())
            .withBody(
              """{
                "status": ${CONFLICT.value()},
                "errorCode": null,
                "userMessage": "User test message",
                "developerMessage": "Developer test message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateEmailDomainNotFound(id: String) {
    stubFor(
      get(urlEqualTo("/email-domains/$id"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(NOT_FOUND.value())
            .withBody(
              """{
                "status": ${NOT_FOUND.value()},
                "errorCode": null,
                "userMessage": "User test message",
                "developerMessage": "Developer test message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateEmailDomain() {
    stubFor(
      post(urlEqualTo("/email-domains"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                    {
                        "id": "45E266FF-8776-48DB-A2F7-4FA927EFE4C8",
                        "domain": "advancecharity.org.uk",
                        "description": "ADVANCE"
                    }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetEmailDomain(id: String) {
    stubFor(
      get("/email-domains/$id")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
                    {
                        "id": "45E266FF-8776-48DB-A2F7-4FA927EFE4C8",
                        "domain": "advancecharity.org.uk",
                        "description": "ADVANCE"
                    }
                  
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetEmailDomains() {
    stubFor(
      get(urlEqualTo("/email-domains"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
                     [
                          {
                              "id": "45E266FF-8776-48DB-A2F7-4FA927EFE4C8",
                              "domain": "advancecharity.org.uk",
                              "description": "ADVANCE"
                          },
                          {
                              "id": "8BB676EE-7531-44BA-9A31-5355BEEAD9DB",
                              "domain": "bidvestnoonan.com",
                              "description": "BIDVESTNOONA"
                          },
                          {
                              "id": "FFDB69CB-1E23-40F2-B94A-11E6A9AE7BBF",
                              "domain": "bsigroup.com",
                              "description": "BSIGROUP"
                          },
                          {
                              "id": "2200A597-F47A-439C-9B84-79ADADD72D7C",
                              "domain": "careuk.com",
                              "description": "CAREUK"
                          }
                      ]
                  
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetRoles() {
    stubFor(
      get(urlEqualTo("/roles?adminTypes"))
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
      get(urlEqualTo("/roles/paged?page=3&size=4&sort=roleName,desc&roleName&roleCode&adminTypes"))
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
      get(urlEqualTo("/roles/paged?page=0&size=10&sort=roleName,asc&roleName&roleCode=account&adminTypes"))
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
      get(urlEqualTo("/roles/paged?page=0&size=10&sort=roleName,asc&roleName=manager&roleCode&adminTypes"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBodyPaged()
            )
        )
    )
  }

  fun stubGetAllRolesPagedFilterAdminType() {
    stubFor(
      get(urlEqualTo("/roles/paged?page=0&size=10&sort=roleName,asc&roleName&roleCode&adminTypes=EXT_ADM"))
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
      get(urlEqualTo("/roles/paged?page=0&size=10&sort=roleName,asc&roleName&roleCode&adminTypes=EXT_ADM&adminTypes=DPS_ADM"))
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
      get(urlEqualTo("/roles/paged?page=1&size=10&sort=roleName,asc&roleName=manager&roleCode=account&adminTypes=EXT_ADM&adminTypes=DPS_ADM"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              apiResponseBodyPaged()
            )
        )
    )
  }

  fun apiResponseBody() =
    """
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

  fun stubGetAllRolesPaged() {
    stubFor(
      get(urlEqualTo("/roles/paged?page=0&size=10&sort=roleName,asc&roleName&roleCode&adminTypes"))
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

  fun stubGetUserRoles(userId: UUID) {
    stubFor(
      get(urlEqualTo("/users/$userId/roles"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
                [
                  {
                    "roleCode": "AUDIT_VIEWER",
                    "roleName": "Audit viewer",
                    "roleDescription": null
                  },
                  {
                    "roleCode": "AUTH_GROUP_MANAGER",
                    "roleName": "Auth Group Manager that has more than 30 characters in the role name",
                    "roleDescription": "Gives group manager ability to administer user in there groups"
                  }
                ]
                  
              """.trimIndent()
            )
        )
    )
  }

  fun stubDeleteRoleFromUser(userId: String, role: String) {
    stubFor(
      delete("/users/$userId/roles/$role")
        .willReturn(
          aResponse()
            .withStatus(NO_CONTENT.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubDeleteUserRoleFail(userId: String, role: String, status: HttpStatus) {
    stubFor(
      delete("/users/$userId/roles/$role")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User error message",
                "developerMessage": "Developer error message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetAssignableRoles(userId: UUID) {
    stubFor(
      get(urlEqualTo("/users/$userId/assignable-roles"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
                [
                  {
                    "roleCode": "AUDIT_VIEWER",
                    "roleName": "Audit viewer",
                    "roleDescription": null
                  },
                  {
                    "roleCode": "AUTH_GROUP_MANAGER",
                    "roleName": "Auth Group Manager role",
                    "roleDescription": "More information about auth group manager role"
                  }
                ]
                  
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetRolesForRoleName() {
    stubFor(
      get(urlEqualTo("/roles?adminTypes=DPS_ADM"))
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

  fun stubGetRoleDetails(roleCode: String) {
    stubFor(
      get(urlEqualTo("/roles/$roleCode"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
                {
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
      get(urlEqualTo("/roles/$roleCode"))
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
      get(urlEqualTo("/roles/$roleCode"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User message for Get Role details failed",
                "developerMessage": "Developer message for get role details failed",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubPutRoleName(roleCode: String) {
    stubFor(
      put("/roles/$roleCode")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutRoleNameFail(roleCode: String, status: HttpStatus) {
    stubFor(
      put("/roles/$roleCode")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User message for PUT Role Name failed",
                "developerMessage": "Developer message for PUT Role Name failed",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubPutRoleDescription(roleCode: String) {
    stubFor(
      put("/roles/$roleCode/description")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutRoleDescriptionFail(roleCode: String, status: HttpStatus) {
    stubFor(
      put("/roles/$roleCode/description")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User message for PUT Role Description failed",
                "developerMessage": "Developer message for PUT Role Description failed",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubPutRoleAdminType(roleCode: String) {
    stubFor(
      put("/roles/$roleCode/admintype")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutRoleAdminTypeFail(roleCode: String, status: HttpStatus) {
    stubFor(
      put("/roles/$roleCode/admintype")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User message for PUT Role Admin Type failed",
                "developerMessage": "Developer message for PUT Role Admin Type failed",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetGroups() {
    stubFor(
      get(urlEqualTo("/groups"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(OK.value())
            .withBody(
              """
                [
                  {
                    "groupCode": "SITE_1_GROUP_1",
                    "groupName": "Site 1 - Group 1"
                  },
                  {
                    "groupCode": "SITE_2_GROUP_2",
                    "groupName": "Site 2 - Group 2"
                  }
                ]
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetGroupDetails(group: String) {
    stubFor(
      get(urlEqualTo("/groups/$group"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(OK.value())
            .withBody(
              """{
                    "groupCode": "SITE_1_GROUP_2",
                    "groupName": "Site 1 - Group 2",
                    "assignableRoles": [
                      {
                        "roleCode": "GLOBAL_SEARCH",
                        "roleName": "Global Search"
                      },
                      {
                        "roleCode": "LICENCE_RO",
                        "roleName": "Licence Responsible Officer"
                      }
                    ],
                    "children": [
                      {
                        "groupCode": "CHILD_1",
                        "groupName": "Child - Site 1 - Group 2"
                      }
                    ]
                  }  
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetChildGroupDetails(group: String) {
    stubFor(
      get(urlEqualTo("/groups/child/$group"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(OK.value())
            .withBody(
              """{
                    "groupCode": "CHILD_1",
                    "groupName": "Child - Site 1 - Group 2"
                  }  
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetGroupDetailsForUserNotAllowed(group: String, status: HttpStatus) {
    stubFor(
      get(urlEqualTo("/groups/$group"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User message",
                "developerMessage": "Developer message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetChildGroupDetailsForUserNotAllowed(group: String, status: HttpStatus) {
    stubFor(
      get(urlEqualTo("/groups/child/$group"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User message",
                "developerMessage": "Developer message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubPutUpdateChildGroup(group: String) {
    stubFor(
      put("/groups/child/$group")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutUpdateChildGroupFail(group: String, status: HttpStatus) {
    stubFor(
      put("/groups/child/$group")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "Group Not found: Unable to maintain group: Not_A_Group with reason: notfound",
                "developerMessage": "Unable to maintain group: Not_A_Group with reason: notfound",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateGroupNotFound(group: String) {
    stubFor(
      get(urlEqualTo("/groups/$group"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(NOT_FOUND.value())
            .withBody(
              """{
                "status": ${NOT_FOUND.value()},
                "errorCode": null,
                "userMessage": "Group Not found: Unable to get group: $group with reason: notfound",
                "developerMessage": "Unable to get group: $group with reason: notfound",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetNotFound(url: String) {
    stubFor(
      get(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(NOT_FOUND.value())
            .withBody(
              """{
                "status": ${NOT_FOUND.value()},
                "errorCode": null,
                "userMessage": "User message",
                "developerMessage": "Developer message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubUpdateChildGroupNotFound(group: String) {
    stubFor(
      put("/groups/child/$group")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(NOT_FOUND.value())
            .withBody(
              """{
                "status": ${NOT_FOUND.value()},
                "errorCode": null,
               "userMessage": "Child Group Not found: Unable to get group: $group with reason: notfound",
                "developerMessage": "Unable to get group: $group with reason: notfound",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubPutUpdateGroup(group: String) {
    stubFor(
      put("/groups/$group")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubDeleteGroupFromUser(userId: String, group: String) {
    stubFor(
      delete("/users/$userId/groups/$group")
        .willReturn(
          aResponse()
            .withStatus(NO_CONTENT.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubAddGroupToUser(userId: String, group: String) {
    stubFor(
      put("/users/$userId/groups/$group")
        .willReturn(
          aResponse()
            .withStatus(NO_CONTENT.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubDeleteChildGroup(group: String) {
    stubFor(
      delete("/groups/child/$group")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutUpdateGroupFail(group: String, status: HttpStatus) {
    stubFor(
      put("/groups/$group")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User error message",
                "developerMessage": "Developer error message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubDeleteChildGroupFail(group: String, status: HttpStatus) {
    stubFor(
      delete("/groups/child/$group")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User error message",
                "developerMessage": "Developer error message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubDeleteUserGroupFail(userId: String, userGroup: String, status: HttpStatus) {
    stubFor(
      delete("/users/$userId/groups/$userGroup")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User error message",
                "developerMessage": "Developer error message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubAddUserGroupFail(userId: String, userGroup: String, status: HttpStatus) {
    stubFor(
      put("/users/$userId/groups/$userGroup")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "User error message",
                "developerMessage": "Developer error message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateGroup() {
    stubFor(
      post(urlEqualTo("/groups"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
        )
    )
  }

  fun stubCreateGroupFail(status: HttpStatus) {
    stubFor(
      post(urlEqualTo("/groups"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "default message [groupName],100,4] default message [groupCode],30,2]",
                "developerMessage": "Developer test message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateGroupsConflict() {
    stubFor(
      post(urlEqualTo("/groups"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(CONFLICT.value())
            .withBody(
              """{
                "status": ${CONFLICT.value()},
                "errorCode": null,
                "userMessage": "User test message",
                "developerMessage": "Developer test message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateChildGroup() {
    stubFor(
      post(urlEqualTo("/groups/child"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
        )
    )
  }

  fun stubCreateChildrenGroupFail(status: HttpStatus) {
    stubFor(
      post(urlEqualTo("/groups/child"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "default message [groupName],100,4] default message [groupCode],30,2],default message [parentGroupCode],30,2]",
                "developerMessage": "Developer test message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateChildGroupsConflict() {
    stubFor(
      post(urlEqualTo("/groups/child"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(CONFLICT.value())
            .withBody(
              """{
                "status": ${CONFLICT.value()},
                "errorCode": null,
                "userMessage": "User test message",
                "developerMessage": "Developer test message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubCreateChildGroupNotFound() {
    stubFor(
      post("/groups/child")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(NOT_FOUND.value())
            .withBody(
              """{
                "status": ${NOT_FOUND.value()},
                "errorCode": null,
               "userMessage": "Group Not found: Unable to create group: PG with reason: ParentGroupNotFound",
                "developerMessage": "Unable to create group: PG with reason: ParentGroupNotFound",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubValidEmailDomain() {
    stubFor(
      get(urlEqualTo("/validate/email-domain?emailDomain=gov.uk"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              "true"
            )
        )
    )
  }

  fun stubInvalidEmailDomain() {
    stubFor(
      get(urlEqualTo("/validate/email-domain?emailDomain=invaliddomain.com"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              "false".trimIndent()
            )
        )
    )
  }

  fun stubDeleteGroup() {
    stubFor(
      delete(urlEqualTo("/groups/GC_DEL_1"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
        )
    )
  }

  fun stubDeleteGroupsConflict() {
    stubFor(
      delete(urlEqualTo("/groups/GC_DEL_3"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(CONFLICT.value())
            .withBody(
              """{
                "status": ${CONFLICT.value()},
                "errorCode": null,
                "userMessage": "Unable to delete group: GC_DEL_3 with reason: child group exist",
                "developerMessage": "Developer test message",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubDeleteGroupNotFound(group: String) {
    stubFor(
      delete(urlEqualTo("/groups/$group"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(NOT_FOUND.value())
            .withBody(
              """{
                "status": ${NOT_FOUND.value()},
                "errorCode": null,
                "userMessage": "Group Not found: Unable to delete group: $group with reason: notfound",
                "developerMessage": "Unable to delete group: $group with reason: notfound",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetUserGroups(userId: UUID, children: Boolean) {
    stubFor(
      get(urlEqualTo("/users/$userId/groups?children=$children"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(OK.value())
            .withBody(
              """
                [
                  {
                    "groupCode": "SITE_1_GROUP_1",
                    "groupName": "Site 1 - Group 1"
                  },
                  {
                    "groupCode": "SITE_2_GROUP_2",
                    "groupName": "Site 2 - Group 2"
                  }
                ]
              """.trimIndent()
            )
        )
    )
  }

  fun stubPutEnableUser(userId: String) {
    stubFor(
      put("/users/$userId/enable")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withBody(
              """
                 {
                    "username": "user.name",
                    "firstName": "user.firstName",
                    "admin": "user.name",
                    "email": "email@ul.com"
                }
              """.trimIndent()
            )
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutDisableUser(userId: String) {
    stubFor(
      put("/users/$userId/disable")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withBody(
              """
                 {
                    "username": "user.name",
                    "firstName": "user.firstName",
                    "admin": "user.name",
                    "email": "email@ul.com"
                }
              """.trimIndent()
            )
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubPutEnableInvalidUser(userId: String) {
    stubFor(
      put("/users/$userId/enable")
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND.value())
            .withBody(
              """{
                "status": ${NOT_FOUND.value()},
                "errorCode": null,
                "userMessage": "User not found: User $userId not found",
                "developerMessage": "User $userId not found",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  } fun stubPutDisableInvalidUser(userId: String) {
    stubFor(
      put("/users/$userId/disable")
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND.value())
            .withBody(
              """{
                "status": ${NOT_FOUND.value()},
                "errorCode": null,
                "userMessage": "User not found: User $userId not found",
                "developerMessage": "User $userId not found",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubPutEnableFailUserNotInGroup() {
    stubFor(
      put("/users/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .willReturn(
          aResponse()
            .withStatus(FORBIDDEN.value())
            .withBody(
              """{
                "status": ${FORBIDDEN.value()},
                "errorCode": null,
                "userMessage": "User group relationship exception: Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups",
                "developerMessage": "Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }
  fun stubPutDisableFailUserNotInGroup() {
    stubFor(
      put("/users/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/disable")
        .willReturn(
          aResponse()
            .withStatus(FORBIDDEN.value())
            .withBody(
              """{
                "status": ${FORBIDDEN.value()},
                "errorCode": null,
                "userMessage": "User group relationship exception: Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups",
                "developerMessage": "Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups",
                "moreInfo": null
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubNoUsersFound(email: String) {
    stubFor(
      get("/users?email=$email")
        .willReturn(
          aResponse()
            .withStatus(NO_CONTENT.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubUsersByEmail(email: String) {
    stubFor(
      get("/users?email=$email")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
                [
                    {
                        "userId": "5105a589-75b3-4ca0-9433-b96228c1c8f3",
                        "username": "AUTH_ADM",
                        "email": "auth_test2@digital.justice.gov.uk",
                        "firstName": "Auth",
                        "lastName": "Adm",
                        "locked": false,
                        "enabled": true,
                        "verified": true,
                        "lastLoggedIn": "2022-12-01T09:30:07.933161",
                        "inactiveReason": null
                    },
                    {
                        "userId": "9e84f1e4-59c8-4b10-927a-9cf9e9a30791",
                        "username": "AUTH_EXPIRED",
                        "email": "auth_test2@digital.justice.gov.uk",
                        "firstName": "Auth",
                        "lastName": "Expired",
                        "locked": false,
                        "enabled": true,
                        "verified": true,
                        "lastLoggedIn": "2022-12-01T09:30:07.933161",
                        "inactiveReason": "Expired"
                    }
                ]
              """.trimIndent()
            )
        )
    )
  }

  fun stubUsersByUserName(userName: String) {
    stubFor(
      get("/users/$userName")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
               {
                      "userId": "5105a589-75b3-4ca0-9433-b96228c1c8f3",
                      "username": "AUTH_ADM",
                      "email": "auth_test2@digital.justice.gov.uk",
                      "firstName": "Auth",
                      "lastName": "Adm",
                      "locked": false,
                      "enabled": true,
                      "verified": true,
                      "lastLoggedIn": "2022-12-01T09:30:07.933161",
                      "inactiveReason": "Expired"
                  }
              """.trimIndent()
            )
        )
    )
  }

  fun stubNoUsersFoundForUserName(userName: String) {
    stubFor(
      get("/users/$userName")
        .willReturn(
          aResponse()
            .withStatus(NOT_FOUND.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
        )
    )
  }

  fun stubMyAssignableGroups() {
    stubFor(
      get("/users/me/assignable-groups")
        .willReturn(
          aResponse()
            .withStatus(OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
               [
                   {
                       "groupCode": "SITE_1_GROUP_1",
                       "groupName": "Site 1 - Group 1"
                   },
                   {
                       "groupCode": "SITE_1_GROUP_2",
                       "groupName": "Site 1 - Group 2"
                   }
               ]
              """.trimIndent()
            )
        )
    )
  }
}

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
            .withStatus(HttpStatus.CONFLICT.value())
            .withBody(
              """{
                "status": ${HttpStatus.CONFLICT.value()},
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
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withBody(
              """{
                "status": ${HttpStatus.NOT_FOUND.value()},
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
            .withStatus(HttpStatus.OK.value())
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
            .withStatus(HttpStatus.OK.value())
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
            .withStatus(HttpStatus.OK.value())
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

  fun stubGetGroupDetails(group: String) {
    stubFor(
      get(urlEqualTo("/groups/$group"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(HttpStatus.OK.value())
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

  fun stubGetGroupDetailsForUserNotNotAllowed(group: String, status: HttpStatus) {
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
                "userMessage": "Auth maintain group relationship exception: Unable to maintain group: SITE_1_GROUP_2 with reason: Group not with your groups",
                "developerMessage": "Unable to maintain group: SITE_1_GROUP_2 with reason: Group not with your groups",
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
            .withStatus(HttpStatus.OK.value())
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
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withBody(
              """{
                "status": ${HttpStatus.NOT_FOUND.value()},
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

  fun stubUpdateChildGroupNotFound(group: String) {
    stubFor(
      put("/groups/child/$group")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withBody(
              """{
                "status": ${HttpStatus.NOT_FOUND.value()},
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
            .withStatus(HttpStatus.OK.value())
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
            .withStatus(HttpStatus.CONFLICT.value())
            .withBody(
              """{
                "status": ${HttpStatus.CONFLICT.value()},
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
            .withStatus(HttpStatus.CONFLICT.value())
            .withBody(
              """{
                "status": ${HttpStatus.CONFLICT.value()},
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
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withBody(
              """{
                "status": ${HttpStatus.NOT_FOUND.value()},
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
}

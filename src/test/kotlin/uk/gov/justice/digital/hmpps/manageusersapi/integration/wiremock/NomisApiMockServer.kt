package uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedCentralAdminUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedGeneralUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedLocalAdminUserRequest

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

  fun stubFindUserByUsernameNoEmail(username: String) {
    stubFor(
      get("/users/$username")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """ {
                "username": "$username",
                "staffId": 123456,
                "firstName": "Nomis",
                "lastName": "Take",
                "activeCaseloadId": "MDI",
                "accountStatus": "OPEN",
                "accountType": "GENERAL",
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
                "username": "TEST_ADM",
                "firstName": "Testadm",
                "lastName": "User",
                "primaryEmail": "testadm@test.com",
                "activeCaseloadId" : "CADM_I",
                "accountType": "ADMIN",
                "staffId": 102,
                "accountStatus": "EXPIRED",
                "dpsRoleCodes": [],
                "accountNonLocked": true,
                "credentialsNonExpired": false,
                "enabled": true,
                "admin": true,
                "active": false,
                "staffStatus": "ACTIVE"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubCreateLinkedCentralAdminUser(request: CreateLinkedCentralAdminUserRequest) {
    stubFor(
      post(urlEqualTo("/users/link-admin-account/${request.existingUsername}")).withRequestBody(
        WireMock.containing(
          """
              {"username":"TEST_USER_ADM"}
          """.trimIndent(),
        ),
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
            .withBody(
              """
                {
                    "staffId": 100,
                    "firstName": "First",
                    "lastName": "Last",
                    "status": "ACTIVE",
                    "primaryEmail": "f.l@justice.gov.uk",
                    "generalAccount": {
                        "username": "TESTUSER1",
                        "active": false,
                        "accountType": "GENERAL",
                        "activeCaseload": {
                            "id": "BXI",
                            "name": "Brixton (HMP)",
                            "function": "GENERAL"
                        },
                        "caseloads": [
                            {
                                "id": "NWEB",
                                "name": "Nomis-web Application",
                                "function": "GENERAL"
                            },
                            {
                                "id": "BXI",
                                "name": "Brixton (HMP)",
                                "function": "GENERAL"
                            }
                        ]
                    },
                    "adminAccount": {
                        "username": "TESTUSER1_ADM",
                        "active": false,
                        "accountType": "ADMIN",
                        "activeCaseload": {
                            "id": "CADM_I",
                            "name": "Central Administration Caseload For Hmps",
                            "function": "GENERAL"
                        },
                        "caseloads": [
                            {
                                "id": "NWEB",
                                "name": "Nomis-web Application",
                                "function": "GENERAL"
                            },
                            {
                                "id": "CADM_I",
                                "name": "Central Administration Caseload For Hmps",
                                "function": "GENERAL"
                            }
                        ]
                    }
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubCreateLinkedLocalAdminUser(request: CreateLinkedLocalAdminUserRequest) {
    stubFor(
      post(urlEqualTo("/users/link-local-admin-account/${request.existingUsername}")).withRequestBody(
        WireMock.containing(
          """
          {"username":"TEST_USER_ADM","localAdminGroup":"MDI"}
          """.trimIndent(),
        ),
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
            .withBody(
              """
                {
                    "staffId": 100,
                    "firstName": "First",
                    "lastName": "Last",
                    "status": "ACTIVE",
                    "primaryEmail": "f.l@justice.gov.uk",
                    "generalAccount": {
                        "username": "TESTUSER1",
                        "active": false,
                        "accountType": "GENERAL",
                        "activeCaseload": {
                            "id": "BXI",
                            "name": "Brixton (HMP)",
                            "function": "GENERAL"
                        },
                        "caseloads": [
                            {
                                "id": "NWEB",
                                "name": "Nomis-web Application",
                                "function": "GENERAL"
                            },
                            {
                                "id": "BXI",
                                "name": "Brixton (HMP)",
                                "function": "GENERAL"
                            }
                        ]
                    },
                    "adminAccount": {
                        "username": "TESTUSER1_ADM",
                        "active": false,
                        "accountType": "ADMIN",
                        "activeCaseload": {
                            "id": "CADM_I",
                            "name": "Central Administration Caseload For Hmps",
                            "function": "GENERAL"
                        },
                        "caseloads": [
                            {
                                "id": "NWEB",
                                "name": "Nomis-web Application",
                                "function": "GENERAL"
                            },
                            {
                                "id": "CADM_I",
                                "name": "Central Administration Caseload For Hmps",
                                "function": "GENERAL"
                            }
                        ]
                    }
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubCreateLinkedGeneralUser(request: CreateLinkedGeneralUserRequest) {
    stubFor(
      post(urlEqualTo("/users/link-general-account/${request.existingAdminUsername}")).withRequestBody(
        WireMock.containing(
          """
          {"username":"TESTUSER1_GEN","defaultCaseloadId":"BXI"}
          """.trimIndent(),
        ),
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
            .withBody(
              """
                {
                    "staffId": 100,
                    "firstName": "First",
                    "lastName": "Last",
                    "status": "ACTIVE",
                    "primaryEmail": "f.l@justice.gov.uk",
                    "generalAccount": {
                        "username": "TESTUSER1_GEN",
                        "active": false,
                        "accountType": "GENERAL",
                        "activeCaseload": {
                            "id": "BXI",
                            "name": "Brixton (HMP)",
                            "function": "GENERAL"
                        },
                        "caseloads": [
                            {
                                "id": "NWEB",
                                "name": "Nomis-web Application",
                                "function": "GENERAL"
                            },
                            {
                                "id": "BXI",
                                "name": "Brixton (HMP)",
                                "function": "GENERAL"
                            }
                        ]
                    },
                    "adminAccount": {
                        "username": "TESTUSER1_ADM",
                        "active": false,
                        "accountType": "ADMIN",
                        "activeCaseload": {
                            "id": "CADM_I",
                            "name": "Central Administration Caseload For Hmps",
                            "function": "GENERAL"
                        },
                        "caseloads": [
                            {
                                "id": "NWEB",
                                "name": "Nomis-web Application",
                                "function": "GENERAL"
                            },
                            {
                                "id": "CADM_I",
                                "name": "Central Administration Caseload For Hmps",
                                "function": "GENERAL"
                            }
                        ]
                    }
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
                "username": "TEST_GEN",
                "firstName": "Testgen",
                "lastName": "User",
                "primaryEmail": "testgen@test.com",
                "activeCaseloadId": "MDI",
                "accountType": "GENERAL",
                "staffId": 101,
                "accountStatus": "EXPIRED",
                "dpsRoleCodes": [],
                "accountNonLocked": true,
                "credentialsNonExpired": false,
                "enabled": true,
                "admin": false,
                "active": false,
                "staffStatus": "ACTIVE"
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
                "username": "TEST_LADM",
                "firstName": "Testladm",
                "lastName": "User",
                "primaryEmail": "testladm@test.com",
                "activeCaseloadId": "MDI",
                "accountType": "ADMIN",
                "staffId": 100,
                "accountStatus": "EXPIRED",
                "dpsRoleCodes": [],
                "accountNonLocked": true,
                "credentialsNonExpired": false,
                "enabled": true,
                "admin": true,
                "active": false,
                "staffStatus": "ACTIVE"
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
                "userMessage": "User already exists",
                "developerMessage": "User TEST21 already exists"
               }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubNotFoundOnPostTo(url: String) {
    stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withBody(
              """{
                "status": ${HttpStatus.NOT_FOUND.value()},
                "userMessage": "User test message",
                "developerMessage": "Developer test message",
               }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubConflictOnPostTo(url: String) {
    stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.CONFLICT.value())
            .withBody(
              """{
                "status": ${HttpStatus.CONFLICT.value()},
                "userMessage": "User test message",
                "developerMessage": "Developer test message",
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

  fun stubSpecifiedHttpStatusOnPostTo(url: String, status: HttpStatus) {
    stubFor(
      post(urlEqualTo(url))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "userMessage": "User test message",
                "developerMessage": "Developer test message",
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

  fun stubGetUserCaseloads(username: String) {
    stubFor(
      get("/users/$username/caseloads")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              getUserCaseloadDetail(username),
            ),
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
            .withBody(getUserRoleDetailResponse()),
        ),
    )
  }

  fun stubFindUserBasicDetailsByUsername(username: String) {
    stubFor(
      get("/users/basic/${username.uppercase()}")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """ {
                "username": "$username",
                "staffId": 123456,
                "firstName": "Nomis",
                "lastName": "Take",
                "activeCaseloadId": "MDI",
                "accountStatus": "OPEN",
                "primaryEmail": "nomis.usergen@digital.justice.gov.uk",
                "enabled": true
               }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubFindUserByUsername(username: String) {
    stubFor(
      get("/users/${username.uppercase()}")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """ {
                "username": "$username",
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

  fun stubFindUserCaseloads(username: String) {
    stubFor(
      get("/me/caseloads")
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """  {
                     "username": "$username",
                     "active": true,
                     "accountType": "GENERAL",
                     "activeCaseload": {
                       "id": "WWI",
                       "name": "WANDSWORTH (HMP)",
                       "function": "GENERAL"
                     },
                     "caseloads": [
                       {
                         "id": "WWI",
                         "name": "WANDSWORTH (HMP)",
                         "function": "GENERAL"
                       }
                     ]
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
                        "name": "Moorland (HMP & YOI)",
                        "function": "GENERAL"
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
                        "name": "Central Administration Caseload For Hmps",
                        "function": "GENERAL"
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

  fun stubFindUsersByFilter(url: String, status: HttpStatus) {
    stubFor(
      get(url)
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(status.value())
            .withBody(
              """ 
                {
                "content": [
                  {
                      "username": "IMS_ADMIN_LOCAL",
                      "staffId": 67,
                      "firstName": "Ims",
                      "lastName": "Admin",
                      "active": true,
                      "status": "OPEN",
                      "locked": false,
                      "expired": false,
                      "activeCaseload": {
                          "id": "BAI",
                          "name": "Belmarsh (HMP)",
                          "function": "GENERAL"
                      },
                      "dpsRoleCount": 1,
                      "email": null,
                      "staffStatus": "ACTIVE"
                  }
                ],
                "pageable": {
                    "pageNumber": 0,
                    "pageSize": 10,
                    "sort": {
                        "empty": false,
                        "sorted": true,
                        "unsorted": false
                    },
                    "offset": 0,
                    "paged": true,
                    "unpaged": false
                },
                "last": false,
                "totalElements": 69,
                "totalPages": 7,
                "size": 10,
                "number": 0,
                "sort": {
                    "empty": false,
                    "sorted": true,
                    "unsorted": false
                },
                "first": true,
                "numberOfElements": 10,
                "empty": false
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubDownloadUsersByFilter(url: String, status: HttpStatus) {
    stubFor(
      get(url)
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(status.value())
            .withBody(
              """ 
                [
                  {
                    "username": "IMS_ADMIN_LOCAL",
                    "staffId": 67,
                    "firstName": "Ims",
                    "lastName": "Admin",
                    "active": true,
                    "status": "OPEN",
                    "locked": false,
                    "expired": false,
                    "activeCaseload": {
                        "id": "BAI",
                        "name": "Belmarsh (HMP)",
                        "function": "GENERAL"
                    },
                    "dpsRoleCount": 1,
                    "email": null,
                    "staffStatus": "ACTIVE"
                  }
                ]
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubPostUserRoles(username: String, body: String) {
    stubFor(
      post("/users/$username/roles?caseloadId=NWEB")
        .withRequestBody(containing(body))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(getUserRoleDetailResponse()),
        ),
    )
  }

  fun stubDeleteUserRole(username: String, roleCode: String) {
    stubFor(
      delete("/users/$username/roles/$roleCode?caseloadId=NWEB")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(getUserRoleDetailResponse()),
        ),
    )
  }

  fun stubDownloadAdminUsersByFilter(url: String, status: HttpStatus) {
    stubFor(
      get(url)
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(status.value())
            .withBody(
              """ 
                [
                  {
                    "username": "IMS_ADMIN_LOCAL",
                    "staffId": 67,
                    "firstName": "Ims",
                    "lastName": "Admin",
                    "active": true,
                    "status": "OPEN",
                    "locked": false,
                    "expired": false,
                    "activeCaseload": {
                        "id": "BAI",
                        "name": "Belmarsh (HMP)",
                        "function": "GENERAL"
                    },
                    "dpsRoleCount": 1,
                    "email": null,
                    "staffStatus": "ACTIVE",
                    "groups": [
                      {
                          "id": "BXI",
                          "name": "Brixton (HMP)",
                          "function": "GENERAL"
                      }
                    ]
                  }
                ]
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubUsersByRoleAndActiveCaseload(roles: List<String>, caseload: String) {
    stubFor(
      get(
        urlPathEqualTo("/users"),
      )
        .withQueryParam("status", equalTo("ACTIVE"))
        .withQueryParam("activeCaseload", equalTo(caseload))
        .withQueryParam("accessRoles", equalTo(roles.joinToString(",")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              usersByRoleAndCaseloadJson(
                firstName = "Maggie",
                lastName = "Simpson",
                caseload = caseload,
              ),
            ),
        ),
    )
  }

  fun stubUsersByRoleAndCaseload(roles: List<String>, caseload: String) {
    stubFor(
      get(
        urlPathEqualTo("/users"),
      )
        .withQueryParam("status", equalTo("ACTIVE"))
        .withQueryParam("caseload", equalTo(caseload))
        .withQueryParam("accessRoles", equalTo(roles.joinToString(",")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              usersByRoleAndCaseloadJson(
                firstName = "Homer",
                lastName = "Simpson",
                caseload = caseload,
              ),
            ),
        ),
    )
  }

  fun stubGetCaseloads(url: String) {
    stubFor(
      get(url)
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withStatus(HttpStatus.OK.value())
            .withBody(
              """ 
                [
                  {
                    "id": "BXI",
                    "name": "Brixton (HMP)",
                    "function": "GENERAL"
                  }
                ]
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubPostUserCaseloads(username: String, body: String) {
    stubFor(
      post("/users/$username/caseloads")
        .withRequestBody(containing(body))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              getUserCaseloadDetail(username),
            ),
        ),
    )
  }

  fun stubDeleteUserCaseloads(username: String, caseloadId: String) {
    stubFor(
      delete("/users/$username/caseloads/$caseloadId")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              getUserCaseloadDetail(username),
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

  fun stubPut(url: String, status: HttpStatus) {
    stubFor(
      put(url)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "Nomis User message for PUT failed",
                "developerMessage": "Developer Nomis user message for PUT failed"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  private fun getUserRoleDetailResponse(): String = """
      {
        "username": "BOB",
        "active": true,
        "accountType": "ADMIN",
        "activeCaseload": {
        "id": "CADM_I",
        "name": "Central Administration Caseload For Hmps",
        "function": "GENERAL"
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

  private fun getUserCaseloadDetail(username: String): String = """ 
      {
        "username": "$username",
        "active": true,
        "accountType": "GENERAL",
        "activeCaseload": {
           "id": "WWI",
           "name": "WANDSWORTH (HMP)",
           "function": "GENERAL"
          },
          "caseloads": [
           {
             "id": "WWI",
             "name": "WANDSWORTH (HMP)",
             "function": "GENERAL"
           }
          ]
      }
  """.trimIndent()

  private fun usersByRoleAndCaseloadJson(firstName: String, lastName: String, caseload: String): String = """
            {
              "content": [
                {
                  "username": "$firstName.$lastName",
                  "staffId": 100.0,
                  "firstName": "$firstName",
                  "lastName": "$lastName",
                  "active": true,
                  "status": "ACTIVE",
                  "locked": false,
                  "expired": false,
                  "activeCaseload": {
                    "id": "$caseload",
                    "name": "$caseload (HMP)",
                    "function": "GENERAL"
                  },
                  "dpsRoleCount": 0.0,
                  "staffStatus": "ACTIVE"
                }
              ],
              "pageable": {
                "pageNumber": 0.0,
                "pageSize": 10.0,
                "sort": {
                  "empty": false,
                  "sorted": true,
                  "unsorted": false
                },
                "offset": 0.0,
                "unpaged": false,
                "paged": true
              },
              "last": true,
              "totalPages": 1.0,
              "totalElements": 1.0,
              "first": true,
              "size": 10.0,
              "number": 0.0,
              "sort": {
                "empty": false,
                "sorted": true,
                "unsorted": false
              },
              "numberOfElements": 1.0,
              "empty": false
            }
  """.trimIndent()
}

package uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CreateLinkedAdminUserRequest

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

  fun stubCreateLinkedCentralAdminUser(request: CreateLinkedAdminUserRequest) {
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
                            "name": "Brixton (HMP)"
                        },
                        "caseloads": [
                            {
                                "id": "NWEB",
                                "name": "Nomis-web Application"
                            },
                            {
                                "id": "BXI",
                                "name": "Brixton (HMP)"
                            }
                        ]
                    },
                    "adminAccount": {
                        "username": "TESTUSER1_ADM",
                        "active": false,
                        "accountType": "ADMIN",
                        "activeCaseload": {
                            "id": "CADM_I",
                            "name": "Central Administration Caseload For Hmps"
                        },
                        "caseloads": [
                            {
                                "id": "NWEB",
                                "name": "Nomis-web Application"
                            },
                            {
                                "id": "CADM_I",
                                "name": "Central Administration Caseload For Hmps"
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

  fun stubCreateLinkedCentralAdminUserDuplicateConflict() {
    stubFor(
      post(urlEqualTo("/users/link-admin-account/TEST_USER"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.CONFLICT.value())
            .withBody(
              """{
                "status": ${HttpStatus.CONFLICT.value()},
                "userMessage": "User already exists: Admin user already exists for this staff member",
                "developerMessage": "Admin user already exists for this staff member"
               }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubCreateLinkedCentralAdminUserExistConflict() {
    stubFor(
      post(urlEqualTo("/users/link-admin-account/TEST_USER"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.CONFLICT.value())
            .withBody(
              """{
                "status": ${HttpStatus.CONFLICT.value()},
                "userMessage": "User already exists: User TEST_USER_ADM already exists",
                "developerMessage": "User TEST_USER_ADM already exists"
               }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubCreateLinkedCentralAdminWhenGeneralUserNotFound() {
    stubFor(
      post(urlEqualTo("/users/link-admin-account/TEST_USER_NOT_FOUND"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.NOT_FOUND.value())
            .withBody(
              """{
                "status": ${HttpStatus.NOT_FOUND.value()},
                "userMessage": "User not found: Linked User Account TEST_USER_NOT_FOUND not found",
                "developerMessage": "Linked User Account TEST_USER_NOT_FOUND not found"
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

  fun stubCreateLinkedCentralAdminUserWithErrorFail(status: HttpStatus) {
    stubFor(
      post(urlEqualTo("/users/link-admin-account/TEST_USER"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(status.value())
            .withBody(
              """{
                "status": ${status.value()},
                "errorCode": null,
                "userMessage": "Validation failure: General user name is required",
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

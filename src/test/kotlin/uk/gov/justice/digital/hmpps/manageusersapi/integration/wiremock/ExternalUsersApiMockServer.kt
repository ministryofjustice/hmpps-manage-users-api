package uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
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
}

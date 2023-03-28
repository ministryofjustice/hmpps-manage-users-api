package uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource
import java.util.UUID

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
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/auth/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status),
      ),
    )
  }

  fun stubForNewToken() {
    stubFor(
      post(urlEqualTo("/auth/api/new-token"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "text/plain;charset=UTF-8")))
            .withBody(
              "a25adf13-dbed-4a19-ad07-d1cd95b12500".trimIndent(),
            ),
        ),
    )
  }

  fun stubResetTokenForUser(userId: String) {
    stubFor(
      post(urlEqualTo("/auth/api/token/reset/${userId.lowercase()}"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "text/plain;charset=UTF-8")))
            .withBody(
              "a25adf13-dbed-4a19-ad07-d1cd95b12500".trimIndent(),
            ),
        ),
    )
  }

  fun stubForTokenByEmailType() {
    stubFor(
      post(urlEqualTo("/auth/api/token/email-type"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "text/plain;charset=UTF-8")))
            .withBody(
              "a25adf13-dbed-4a19-ad07-d1cd95b12500".trimIndent(),
            ),
        ),
    )
  }

  fun stubAzureUserByUsername(username: String) {
    stubFor(
      get("/auth/api/azureuser/$username")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                "username": "CE232D07-40C3-47C6-9903-613BB31132AF",
                "enabled": true,
                "firstName": "Azure",
                "lastName": "User",
                "email": "azure.user@justice.gov.uk"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubUserByUsernameAndSource(username: String, source: AuthSource, uuid: UUID) {
    stubFor(
      get("/auth/api/user/$username/${source.name}")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """{
                "username": "$username",
                "active": true,
                "name": "Test User",
                "authSource": "${source.name}",
                "staffId": 1,
                "activeCaseLoadId": "MDI",
                "userId": "1234",
                "uuid": "$uuid"
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubUserEmail(username: String, unverifiedParam: Boolean = false, verifiedEmail: Boolean = true) {
    stubFor(
      get("/auth/api/user/$username/authEmail?unverified=$unverifiedParam")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.OK.value())
            .withBody(
              """
                {
                  "email": "User.FromAuth@digital.justice.gov.uk",
                  "username": "$username",
                  "verified": $verifiedEmail
                }
              """.trimIndent(),
            ),
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
                "userMessage": "Auth User message for GET failed",
                "developerMessage": "Developer Auth user message for GET failed",
                "moreInfo": null
               }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubUserEmails() {
    stubFor(
      post("/auth/api/prisonuser/email")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.OK.value())
            .withBody(
              """
                [ {
                  "email": "First.Last@digital.justice.gov.uk",
                  "username": "NUSER_GEN",
                  "verified": true
                }]
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubServiceDetailsByServiceCode(serviceCode: String) {
    stubFor(
      get("/auth/api/services/$serviceCode")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(HttpStatus.OK.value())
            .withBody(
              """
                {
                    "code": "$serviceCode",
                    "name": "Digital Prison Service",
                    "description": "View and Manage Offenders in Prison (Old name was NEW NOMIS)",
                    "contact": "feedback@digital.justice.gov.uk",
                    "url": "http://localhost:3000"
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubUserSearchNoResults() {
    stubFor(
      get("/auth/api/user/search")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
               {
                  "content": [],
                  "pageable": {
                      "sort": {
                          "empty": false,
                          "sorted": true,
                          "unsorted": false
                      },
                      "offset": 0,
                      "pageSize": 10,
                      "pageNumber": 0,
                      "paged": true,
                      "unpaged": false
                  },
                  "totalPages": 0,
                  "totalElements": 0,
                  "last": true,
                  "size": 10,
                  "number": 0,
                  "sort": {
                      "empty": false,
                      "sorted": true,
                      "unsorted": false
                  },
                  "first": true,
                  "numberOfElements": 0,
                  "empty": true
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubUserSearchWithResults(searchParams: String) {
    stubFor(
      get("/auth/api/user/search?$searchParams")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "content": [
                  {
                    "userId": "006a9299-ef3d-4990-8604-13cefac706b5",
                    "username": "TESTER.MCTESTY@DIGITAL.JUSTICE.GOV.UK",
                    "email": "tester.mctesty@digital.justice.gov.uk",
                    "firstName": "Tester1",
                    "lastName": "McTester1",
                    "locked": false,
                    "enabled": true,
                    "verified": true,
                    "lastLoggedIn": "2022-12-14T10:23:04.915132",
                    "inactiveReason": null
                  },
                  {
                    "userId": "bc7098ed-948e-456d-8b21-3afb0257aa23",
                    "username": "TESTER.MCTESTY2@DIGITAL.JUSTICE.GOV.UK",
                    "email": "tester.mctesty2@digital.justice.gov.uk",
                    "firstName": "Tester2",
                    "lastName": "McTester2",
                    "locked": false,
                    "enabled": true,
                    "verified": true,
                    "lastLoggedIn": "2022-12-14T13:57:27.85401",
                    "inactiveReason": null
                 }
               ],
               "pageable": {
                 "sort": {
                       "empty": false,
                       "sorted": true,
                       "unsorted": false
                   },
                   "offset": 0,
                   "pageSize": 10,
                   "pageNumber": 0,
                   "paged": true,
                   "unpaged": false
               },
               "totalPages": 19,
               "totalElements": 185,
               "last": false,
               "size": 10,
               "number": 0,
               "sort": {
                   "empty": false,
                   "sorted": true,
                   "unsorted": false
               },
               "first": true,
              "numberOfElements": 2,
              "empty": false
            }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubUserSearchEncodedQueryParams(encodedName: String) {
    stubFor(
      get("/auth/api/user/search?name=$encodedName&status=ALL&page=0&size=10")
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                "content": [
                  {
                    "userId": "006a9299-ef3d-4990-8604-13cefac706b5",
                    "username": "TESTER.MCTESTY+EMAIL@DIGITAL.JUSTICE.GOV.UK",
                    "email": "tester.mctesty+email@digital.justice.gov.uk",
                    "firstName": "Tester1",
                    "lastName": "McTester1",
                    "locked": false,
                    "enabled": true,
                    "verified": true,
                    "lastLoggedIn": "2022-12-14T10:23:04.915132",
                    "inactiveReason": null
                  }
                ],
                "pageable": {
                  "sort": {
                    "empty": false,
                    "sorted": true,
                    "unsorted": false
                  },
                  "offset": 0,
                  "pageSize": 10,
                  "pageNumber": 0,
                  "paged": true,
                  "unpaged": false
                },
                "totalPages": 19,
                "totalElements": 185,
                "last": false,
                "size": 10,
                "number": 0,
                "sort": {
                  "empty": false,
                  "sorted": true,
                  "unsorted": false
                },
                "first": true,
                "numberOfElements": 2,
                "empty": false
              }
              """.trimIndent(),
            ),
        ),
    )
  }
}

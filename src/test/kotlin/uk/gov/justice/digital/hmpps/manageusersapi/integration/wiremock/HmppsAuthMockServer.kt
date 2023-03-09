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
      get("/auth/api/user?username=$username&source=${source.name}")
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
}

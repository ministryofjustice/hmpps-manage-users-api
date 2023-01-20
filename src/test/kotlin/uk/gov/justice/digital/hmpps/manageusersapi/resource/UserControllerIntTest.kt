package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import java.util.UUID

class UserControllerIntTest : IntegrationTestBase() {
  @Nested
  inner class FindByUsername {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/users/AUTH_ADM")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun ` should fail with not_found`() {
      externalUsersApiMockServer.stubGetFail("/users/AUTH_ADM", NOT_FOUND)
      webTestClient.get().uri("/users/AUTH_ADM")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"]).isEqualTo(NOT_FOUND.value())
          assertThat(it["userMessage"]).isEqualTo("Account for username AUTH_ADM not found")
          assertThat(it["developerMessage"]).isEqualTo("Account for username AUTH_ADM not found")
        }
    }

    @Test
    fun ` should fail with not_found for azure user`() {
      val username = UUID.randomUUID()
      externalUsersApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      hmppsAuthMockServer.stubGetFail("/auth/api/azureuser/$username", NOT_FOUND)
      webTestClient.get().uri("/users/$username")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"]).isEqualTo(NOT_FOUND.value())
          assertThat(it["userMessage"]).isEqualTo("Account for username $username not found")
          assertThat(it["developerMessage"]).isEqualTo("Account for username $username not found")
        }
      hmppsAuthMockServer.verify(getRequestedFor(urlEqualTo("/auth/api/azureuser/$username")))
    }

    @Test
    fun ` external user found success`() {
      externalUsersApiMockServer.stubUserByUsername("EXT_ADM")
      webTestClient.get().uri("/users/EXT_ADM")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to "EXT_ADM",
              "active" to true,
              "name" to "Ext Adm",
              "authSource" to "auth",
              "userId" to "5105a589-75b3-4ca0-9433-b96228c1c8f3",
              "uuid" to "5105a589-75b3-4ca0-9433-b96228c1c8f3",
            )
          )
        }
    }

    @Test
    fun ` azure user found success`() {
      val username = "ce232d07-40c3-47c6-9903-613bb31132af"
      externalUsersApiMockServer.stubNoUsersFoundForUsername(username)
      hmppsAuthMockServer.stubAzureUserByUsername(username)
      webTestClient.get().uri("/users/$username")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to "CE232D07-40C3-47C6-9903-613BB31132AF",
              "active" to true,
              "name" to "Azure User",
              "authSource" to "azuread",
              "userId" to "azure.user@justice.gov.uk",
              "uuid" to "76ed3c80-2fe6-424f-95a4-556e32d749a7",
            )
          )
        }
    }
  }
}

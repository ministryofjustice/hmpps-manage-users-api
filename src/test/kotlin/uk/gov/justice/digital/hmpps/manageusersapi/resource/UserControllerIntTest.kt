package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
    fun ` should fail with not_found for basic username`() {
      val username = "AUTH_ADM"
      externalUsersApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      deliusApiMockServer.stubGetFail("/users/$username/details", NOT_FOUND)
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
      hmppsAuthMockServer.verify(0, getRequestedFor(urlEqualTo("/auth/api/azureuser/$username")))
    }

    @Test
    fun ` should fail with not_found for UUID username`() {
      val username = UUID.randomUUID()
      externalUsersApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      hmppsAuthMockServer.stubGetFail("/auth/api/azureuser/$username", NOT_FOUND)
      deliusApiMockServer.stubGetFail("/users/$username/details", NOT_FOUND)
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
    }

    @Test
    fun ` should fail with not_found for email address username`() {
      val username = "testing@digital.justice.gov.uk"
      externalUsersApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      webTestClient.get().uri("/users/$username")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isNotFound
      nomisApiMockServer.verify(0, getRequestedFor(urlEqualTo("/users/$username")))
      hmppsAuthMockServer.verify(0, getRequestedFor(urlEqualTo("/auth/api/azureuser/$username")))
      deliusApiMockServer.verify(0, getRequestedFor(urlEqualTo("/users/$username/details")))
    }

    @Test
    fun ` external user found success`() {
      val username = "EXT_ADM"
      externalUsersApiMockServer.stubUserByUsername(username)
      webTestClient.get().uri("/users/$username")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username,
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
    fun ` nomis user found success`() {
      val username = "NUSER_GEN"
      externalUsersApiMockServer.stubNoUsersFoundForUsername(username)
      nomisApiMockServer.stubFindUserByUsername(username)
      webTestClient.get().uri("/users/$username")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username.uppercase(),
              "active" to true,
              "name" to "Nomis Take",
              "authSource" to "nomis",
              "userId" to "123456",
              "staffId" to 123456,
            )
          )
        }
    }

    @Test
    fun ` azure user found success`() {
      val username = "ce232d07-40c3-47c6-9903-613bb31132af"
      externalUsersApiMockServer.stubNoUsersFoundForUsername(username)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
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
              "username" to username.uppercase(),
              "active" to true,
              "name" to "Azure User",
              "authSource" to "azuread",
              "userId" to "azure.user@justice.gov.uk",
              "uuid" to "76ed3c80-2fe6-424f-95a4-556e32d749a7",
            )
          )
        }
    }

    @Test
    fun ` delius user found success`() {
      val username = "deliususer"
      externalUsersApiMockServer.stubNoUsersFoundForUsername(username)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      deliusApiMockServer.stubGetUser(username)
      webTestClient.get().uri("/users/$username")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username.uppercase(),
              "active" to true,
              "name" to "Delius Smith",
              "authSource" to "delius",
              "userId" to "1234567890",
            )
          )
        }
    }
  }

  @Nested
  inner class MyRoles {
    @Test
    fun `User Me Roles endpoint returns principal user data`() {

      webTestClient
        .get().uri("/users/me/roles")
        .headers(
          setAuthorisation(
            "ITAG_USER",
            listOf("ROLE_MAINTAIN_ACCESS_ROLES", "ROLE_MAINTAIN_OAUTH_USERS", "ROLE_OAUTH_ADMIN")
          )
        )
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("[*].roleCode").value<List<String>> {
          assertThat(it).contains("MAINTAIN_OAUTH_USERS")
          assertThat(it).contains("OAUTH_ADMIN")
        }
    }

    @Test
    fun `User Me Roles endpoint returns principal user data for auth user`() {

      webTestClient
        .get().uri("/users/me/roles")
        .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_GLOBAL_SEARCH")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("[*].roleCode").value<List<String>> {
          assertThat(it).contains("GLOBAL_SEARCH")
        }
    }

    @Test
    fun `User Me Roles endpoint not accessible without valid token`() {
      webTestClient
        .get().uri("/users/me/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }
}

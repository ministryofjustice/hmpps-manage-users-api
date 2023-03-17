package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.auth
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.azuread
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.delius
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.nomis
import java.util.Locale
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
      deliusApiMockServer.stubGetFail("/secure/users/$username/details", NOT_FOUND)
      webTestClient.get().uri("/users/$username")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["error"]).isEqualTo("Not Found")
          assertThat(it["error_description"]).isEqualTo("Account for username $username not found")
          assertThat(it["field"]).isEqualTo("username")
        }
      hmppsAuthMockServer.verify(0, getRequestedFor(urlEqualTo("/auth/api/azureuser/$username")))
    }

    @Test
    fun ` should fail with not_found for UUID username`() {
      val username = UUID.randomUUID()
      externalUsersApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      hmppsAuthMockServer.stubGetFail("/auth/api/azureuser/$username", NOT_FOUND)
      deliusApiMockServer.stubGetFail("/secure/users/$username/details", NOT_FOUND)
      webTestClient.get().uri("/users/$username")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["error"]).isEqualTo("Not Found")
          assertThat(it["error_description"]).isEqualTo("Account for username $username not found")
          assertThat(it["field"]).isEqualTo("username")
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
      deliusApiMockServer.verify(0, getRequestedFor(urlEqualTo("/secure/users/$username/details")))
    }

    @Test
    fun ` external user found success`() {
      val username = "EXT_ADM"
      val uuid = UUID.randomUUID()
      externalUsersApiMockServer.stubUserByUsername(username)
      hmppsAuthMockServer.stubUserByUsernameAndSource(username, auth, uuid)
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
              "uuid" to uuid.toString(),
            ),
          )
        }
    }

    @Test
    fun ` nomis user found success`() {
      val username = "NUSER_GEN"
      val uuid = UUID.randomUUID()

      val userMessage = "User not found: Account for username $username not found"
      val developerMessage = "Account for username $username not found"
      externalUsersApiMockServer.stubGet(OK, "/users/$username", userMessage, developerMessage)
      nomisApiMockServer.stubFindUserByUsername(username)
      hmppsAuthMockServer.stubUserByUsernameAndSource(username, nomis, uuid)
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
              "activeCaseLoadId" to "MDI",
              "uuid" to uuid.toString(),
            ),
          )
        }
    }

    @Test
    fun ` azure user found success`() {
      val username = "ce232d07-40c3-47c6-9903-613bb31132af"
      val uuid = UUID.randomUUID()
      val userMessage = "User not found: Account for username $username not found"
      val developerMessage = "Account for username $username not found"
      externalUsersApiMockServer.stubGet(OK, "/users/$username", userMessage, developerMessage)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      hmppsAuthMockServer.stubAzureUserByUsername(username)
      hmppsAuthMockServer.stubUserByUsernameAndSource(username, azuread, uuid)
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
              "uuid" to uuid.toString(),
            ),
          )
        }
    }

    @Test
    fun ` delius user found success`() {
      val uuid = UUID.randomUUID()
      val username = "deliususer"
      val userMessage = "User not found: Account for username $username not found"
      val developerMessage = "Account for username $username not found"
      externalUsersApiMockServer.stubGet(OK, "/users/$username", userMessage, developerMessage)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      deliusApiMockServer.stubGetUser(username)
      hmppsAuthMockServer.stubUserByUsernameAndSource(username, delius, uuid)
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
              "uuid" to uuid.toString(),
            ),
          )
        }
    }
  }

  @Nested
  inner class MyDetails {
    @Test
    fun `Users Me endpoint returns user data`() {
      val username = "AUTH_ADM"
      val uuid = UUID.randomUUID()
      externalUsersApiMockServer.stubUserByUsername(username)
      hmppsAuthMockServer.stubUserByUsernameAndSource(username, auth, uuid)
      webTestClient
        .get().uri("/users/me")
        .headers(
          setAuthorisation(),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to "AUTH_ADM",
              "active" to true,
              "name" to "Ext Adm",
              "authSource" to "auth",
              "userId" to "5105a589-75b3-4ca0-9433-b96228c1c8f3",
              "uuid" to uuid.toString(),
            ),
          )
        }
    }

    @Test
    fun `Users Me endpoint returns user data if not user`() {
      val username = "basicuser"
      externalUsersApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      deliusApiMockServer.stubGetFail("/secure/users/$username/details", NOT_FOUND)
      webTestClient
        .get().uri("/users/me")
        .headers(
          setAuthorisation("basicuser"),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyEntriesOf(
            mapOf(
              "username" to "basicuser",
            ),
          )
        }
    }

    @Test
    fun `Users Me endpoint not accessible without valid token`() {
      webTestClient
        .get().uri("/users/me")
        .exchange()
        .expectStatus().isUnauthorized
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
            listOf("ROLE_MAINTAIN_ACCESS_ROLES", "ROLE_MAINTAIN_OAUTH_USERS", "ROLE_OAUTH_ADMIN"),
          ),
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

  @Nested
  inner class UserRoles {
    @Test
    fun `No roles for for valid azure user`() {
      val username = "ce232d07-40c3-47c6-9903-613bb31132af".uppercase(Locale.getDefault())
      val userMessage = "User not found: Account for username $username not found"
      val developerMessage = "Account for username $username not found"
      externalUsersApiMockServer.stubGet(OK, "/users/username/$username/roles", userMessage, developerMessage)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      hmppsAuthMockServer.stubAzureUserByUsername(username)
      webTestClient.get().uri("/users/$username/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_INTEL_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<List<Any>> {
          assertThat(it).isEmpty()
        }
    }

    @Test
    fun `Roles of valid nomis user`() {
      val username = "NUSER_GEN"
      val userMessage = "User not found: Account for username $username not found"
      val developerMessage = "Account for username $username not found"
      externalUsersApiMockServer.stubGet(OK, "/users/username/$username/roles", userMessage, developerMessage)
      nomisApiMockServer.stubFindUserByUsername(username)
      webTestClient.get().uri("/users/$username/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_PCMS_USER_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("[*].roleCode").value<List<String>> {
          assertThat(it).contains("MAINTAIN_ACCESS_ROLES")
          assertThat(it).contains("GLOBAL_SEARCH")
          assertThat(it).contains("HMPPS_REGISTERS_MAINTAINER")
          assertThat(it).contains("HPA_USER")
        }
    }

    @Test
    fun ` external user found success`() {
      val username = "EXT_ADM"
      val uuid = UUID.randomUUID()
      externalUsersApiMockServer.stubGetSearchableRoles("/users/username/$username/roles")
      hmppsAuthMockServer.stubUserByUsernameAndSource(username, auth, uuid)
      webTestClient.get().uri("/users/$username/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_PCMS_USER_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("[*].roleCode").value<List<String>> {
          assertThat(it).contains("PF_POLICE")
        }
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/users/username/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should fail with not_found for invalid username`() {
      val username = "AUTH_ADM"
      externalUsersApiMockServer.stubGetFail("/users/username/$username/roles", NOT_FOUND)
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      deliusApiMockServer.stubGetFail("/secure/users/$username/details", NOT_FOUND)
      webTestClient.get().uri("/users/$username/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_PCMS_USER_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["error"]).isEqualTo("Not Found")
          assertThat(it["error_description"]).isEqualTo("Account for username $username not found")
          assertThat(it["field"]).isEqualTo("username")
        }
      hmppsAuthMockServer.verify(0, getRequestedFor(urlEqualTo("/auth/api/azureuser/$username")))
    }
  }

  @Nested
  inner class MappedDeliusRoles {
    @Test
    fun `User Me Roles endpoint returns principal user data for auth user`() {
      webTestClient
        .get().uri("/roles/delius?deliusRoles=TEST_ROLE,TEST_WORKLOAD_MEASUREMENT_ROLE")
        .headers(setAuthorisation("AUTH_ADM", listOf()))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("[*].name").value<List<String>> {
          assertThat(it).contains("ROLE_LICENCE_RO", "ROLE_GLOBAL_SEARCH", "ROLE_WORKLOAD_MEASUREMENT")
        }
    }

    @Test
    fun `User Me Roles endpoint returns principal user data for auth user1`() {
      webTestClient
        .get().uri("/roles/delius?deliusRoles=NON_MAPPED_ROLE")
        .headers(setAuthorisation("AUTH_ADM", listOf()))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("[*].name").value<List<String>> {
          assertThat(it).hasSize(0)
        }
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/roles/delius?deliusRoles=NON_MAPPED_ROLE")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }
}

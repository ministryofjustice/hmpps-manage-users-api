package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
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

  fun stubUserNotFound(
    username: String,
    external: Boolean = true,
    nomis: Boolean = false,
    azure: Boolean = false,
    delius: Boolean = false,
  ) {
    if (external) externalUsersApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
    if (nomis) nomisApiMockServer.stubGetFail("/users/${username.uppercase()}", NOT_FOUND)
    if (azure) hmppsAuthMockServer.stubGetFail("/auth/api/azureuser/$username", NOT_FOUND)
    if (delius) deliusApiMockServer.stubGetFail("/secure/users/$username/details", NOT_FOUND)
  }

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
      stubUserNotFound(username, external = true, nomis = true, delius = true)

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
      val username = UUID.randomUUID().toString()
      stubUserNotFound(username, external = true, nomis = true, azure = true, delius = true)

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
      stubUserNotFound(username, external = true)

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
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, auth, uuid)
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
      stubUserNotFound(username, external = true)
      nomisApiMockServer.stubFindUserBasicDetailsByUsername(username)
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, nomis, uuid)
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
      stubUserNotFound(username, external = true, nomis = true)
      hmppsAuthMockServer.stubAzureUserByUsername(username)
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, azuread, uuid)
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
      stubUserNotFound(username, external = true, nomis = true)
      deliusApiMockServer.stubGetUser(username)
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, delius, uuid)
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
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, auth, uuid)
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
      stubUserNotFound(username, external = true, nomis = true, delius = true)

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
  inner class FindMyEmail {
    @Test
    fun `My email endpoint returns unverified email`() {
      val username = "USER_UNVERIFIED"
      hmppsAuthMockServer.stubUserEmail(username, unverifiedParam = true, verifiedEmail = false)

      webTestClient
        .get().uri("/users/me/email?unverified=true")
        .headers(
          setAuthorisation(
            "USER_UNVERIFIED",
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username,
              "email" to "User.FromAuth@digital.justice.gov.uk",
              "verified" to false,
            ),
          )
        }
    }

    @Test
    fun `My email endpoint returns no content unverified email`() {
      val username = "USER_UNVERIFIED"
      hmppsAuthMockServer.stubUserEmail(username, unverifiedParam = false, verifiedEmail = false)

      webTestClient
        .get().uri("/users/me/email?unverified=false")
        .headers(
          setAuthorisation(
            "USER_UNVERIFIED",
          ),
        )
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `My email endpoint returns verified email`() {
      val username = "USER_VERIFIED"
      hmppsAuthMockServer.stubUserEmail(username)

      webTestClient
        .get().uri("/users/me/email")
        .headers(
          setAuthorisation(
            "USER_VERIFIED",
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username,
              "email" to "User.FromAuth@digital.justice.gov.uk",
              "verified" to true,
            ),
          )
        }
    }
  }

  @Nested
  inner class FindUserEmail {

    @Test
    fun `User email endpoint not accessible without valid token`() {
      webTestClient
        .get().uri("/users/bob/email")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `User email endpoint returns no content for unverified user in auth`() {
      hmppsAuthMockServer.stubUserEmail("AUTH_UNVERIFIED", unverifiedParam = false, verifiedEmail = false)

      webTestClient
        .get().uri("/users/AUTH_UNVERIFIED/email")
        .headers(setAuthorisation("ITAG_USER"))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `User email endpoint returns not found if no user`() {
      val username = "no_user"
      hmppsAuthMockServer.stubGetFail("/auth/api/user/$username/authEmail?unverified=false", NOT_FOUND)
      stubUserNotFound(username, external = true, nomis = true, delius = true)

      webTestClient
        .get().uri("/users/$username/email")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound

      hmppsAuthMockServer.verify(0, getRequestedFor(urlMatching("/auth/api/user\\?username=$username&source=([a-z]*)")))
    }

    @Test
    fun `User email endpoint returns unverified user in auth if param set`() {
      val username = "AUTH_UNVERIFIED"
      hmppsAuthMockServer.stubUserEmail(username, unverifiedParam = true, verifiedEmail = false)

      webTestClient
        .get().uri("/users/$username/email?unverified=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username,
              "email" to "User.FromAuth@digital.justice.gov.uk",
              "verified" to false,
            ),
          )
        }
      hmppsAuthMockServer.verify(0, getRequestedFor(urlEqualTo("/auth/api/user/$username/auth")))
    }

    @Test
    fun `User email endpoint returns verified user email from auth`() {
      val username = "AUTH_USER"
      hmppsAuthMockServer.stubUserEmail("AUTH_USER")

      webTestClient
        .get().uri("/users/$username/email")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username,
              "email" to "User.FromAuth@digital.justice.gov.uk",
              "verified" to true,
            ),
          )
        }
      hmppsAuthMockServer.verify(0, getRequestedFor(urlEqualTo("/auth/api/user/$username/auth")))
    }

    @Test
    fun `User email endpoint returns user data for external user`() {
      val username = "AUTH_USER"
      hmppsAuthMockServer.stubGetFail("/auth/api/user/$username/authEmail?unverified=false", NOT_FOUND)
      externalUsersApiMockServer.stubUserByUsername(username)
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, auth, UUID.randomUUID())

      webTestClient
        .get().uri("/users/$username/email")
        .headers(setAuthorisation(username))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username,
              "email" to "ext_test@digital.justice.gov.uk",
              "verified" to true,
            ),
          )
        }
      hmppsAuthMockServer.verify(1, getRequestedFor(urlEqualTo("/auth/api/user/$username/auth")))
    }

    @Test
    fun `User email endpoint returns user data for nomis user`() {
      val username = "NUSER_GEN1"
      hmppsAuthMockServer.stubGetFail("/auth/api/user/$username/authEmail?unverified=false", NOT_FOUND)
      stubUserNotFound(username, external = true)
      nomisApiMockServer.stubFindUserByUsername(username)
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, nomis, UUID.randomUUID())

      webTestClient
        .get().uri("/users/$username/email")
        .headers(setAuthorisation(username))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username,
              "email" to "nomis.usergen@digital.justice.gov.uk",
              "verified" to true,
            ),
          )
        }
      hmppsAuthMockServer.verify(1, getRequestedFor(urlEqualTo("/auth/api/user/$username/nomis")))
    }

    @Test
    fun `User email endpoint returns empty for nomis user without email`() {
      val username = "NUSER_GEN"
      hmppsAuthMockServer.stubGetFail("/auth/api/user/$username/authEmail?unverified=true", NOT_FOUND)
      stubUserNotFound(username, external = true)
      nomisApiMockServer.stubFindUserByUsernameNoEmail(username.uppercase())
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, nomis, UUID.randomUUID())

      webTestClient
        .get().uri("/users/$username/email?unverified=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username,
              "verified" to false,
            ),
          )
        }
    }

    @Test
    fun `User email endpoint returns user data for delius user`() {
      val username = "delius_smith"
      hmppsAuthMockServer.stubGetFail("/auth/api/user/$username/authEmail?unverified=false", NOT_FOUND)
      stubUserNotFound(username, external = true, nomis = true)
      deliusApiMockServer.stubGetUser(username)
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, delius, UUID.randomUUID())

      webTestClient
        .get().uri("/users/$username/email")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username.uppercase(),
              "email" to "delius.smithy@digital.justice.gov.uk",
              "verified" to true,
            ),
          )
        }
    }

    @Test
    fun `User email endpoint returns user data for azure user`() {
      val username = "ce232d07-40c3-47c6-9903-613bb31132af"
      hmppsAuthMockServer.stubGetFail("/auth/api/user/$username/authEmail?unverified=false", NOT_FOUND)
      stubUserNotFound(username, external = true, nomis = true)
      hmppsAuthMockServer.stubAzureUserByUsername(username)
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, azuread, UUID.randomUUID())

      webTestClient
        .get().uri("/users/$username/email")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to username.uppercase(),
              "email" to "azure.user@justice.gov.uk",
              "verified" to true,
            ),
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
  inner class MyGroups {

    private val userId: UUID = UUID.fromString("5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8")

    @Test
    fun `get user groups when no authority`() {
      webTestClient.get().uri("/users/me/groups")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Users Me groups endpoint returns empty result if user found`() {
      val username = "basicuser"
      stubUserNotFound(username, external = true, nomis = true, delius = true)
      webTestClient
        .get().uri("/users/me/groups")
        .headers(
          setAuthorisation("basicuser"),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").isEmpty
    }

    @Test
    fun ` get user groups with children`() {
      val username = "AUTH_ADM"
      externalUsersApiMockServer.stubUserByUsername(username)
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, auth, userId)
      externalUsersApiMockServer.stubGetUserGroups(userId, true)

      webTestClient
        .get().uri("/users/me/groups")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .json(
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
          """.trimIndent(),
        )
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
      hmppsAuthMockServer.stubUserIdByUsernameAndSource(username, auth, uuid)
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
        .get().uri("/roles/delius")
        .headers(setAuthorisation("AUTH_ADM", listOf()))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          """
                         {
                              "TEST_ROLE": 
                                [
                                    "ROLE_LICENCE_RO",
                                    "ROLE_GLOBAL_SEARCH"
                                ],
                              "TEST_WORKLOAD_MEASUREMENT_ROLE": 
                                [
                                    "ROLE_WORKLOAD_MEASUREMENT"
                                ]
                        }
                        """,
        )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/roles/delius?deliusRoles=NON_MAPPED_ROLE")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }
}

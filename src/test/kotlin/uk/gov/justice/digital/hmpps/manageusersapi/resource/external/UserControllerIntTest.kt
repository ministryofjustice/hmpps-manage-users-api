package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class UserControllerIntTest : IntegrationTestBase() {
  @Nested
  inner class MyAssignableGroups {

    @Test
    fun `Not accessible without valid token`() {
      webTestClient.get().uri("/externalusers/me/assignable-groups")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Responds with groups for authorised user without roles`() {
      externalUsersApiMockServer.stubMyAssignableGroups()

      webTestClient.get().uri("/externalusers/me/assignable-groups")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].groupCode").value<List<String>> { assertThat(it).hasSize(2) }
        .jsonPath("$.[0].groupCode").isEqualTo("SITE_1_GROUP_1")
        .jsonPath("$.[0].groupName").isEqualTo("Site 1 - Group 1")
    }
  }

  @Nested
  inner class DisableExternalUser {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/disable")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/disable")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(mapOf("reason" to "bob")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/disable")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .body(BodyInserters.fromValue(mapOf("reason" to "bob")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `error when no body`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/disable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun disableUser() {
      externalUsersApiMockServer.stubPutDisableUser("2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f")
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f/disable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .body(BodyInserters.fromValue(mapOf("reason" to "bob")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  inner class EnableExternalUser {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun ` should fail with not_found for invalid user id`() {
      externalUsersApiMockServer.stubPutEnableInvalidUser("2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f")
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(it["userMessage"] as String)
            .startsWith("User not found: User 2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f not found")
          assertThat(it["developerMessage"] as String)
            .startsWith("User 2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f not found")
        }
    }

    @Test
    fun `should fail with forbidden  for user not in group`() {
      externalUsersApiMockServer.stubPutEnableFailUserNotInGroup()
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(HttpStatus.FORBIDDEN.value())
          assertThat(it["userMessage"] as String)
            .startsWith("User group relationship exception: Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups")
          assertThat(it["developerMessage"] as String)
            .startsWith("Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups")
        }
    }

    @Test
    fun enableUser() {
      externalUsersApiMockServer.stubPutEnableUser("2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f")
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  inner class MyRoles {

    @Test
    fun `User Me Roles endpoint returns principal user data`() {

      externalUsersApiMockServer.stubMeRolePrincipleUserData()
      webTestClient
        .get().uri("/externalusers/me/roles")
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

      externalUsersApiMockServer.stubMeRolePrincipleUserDataAuthUser()
      webTestClient
        .get().uri("/externalusers/me/roles")
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
        .get().uri("/externalusers/me/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import java.util.UUID

class ExternalUserGroupControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class GetUserGroups {
    private val userId: UUID = UUID.fromString("5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8")

    @Test
    fun `get user groups when no authority`() {
      webTestClient.get().uri("/users/$userId/groups")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `get user groups forbidden when no role`() {
      webTestClient.get().uri("/users/$userId/groups")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get user groups when user does not exist`() {
      externalUsersApiMockServer.stubGetNotFound("/users/$userId/groups?children=true")
      webTestClient.get().uri("/users/$userId/groups")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun ` get user groups with children`() {
      externalUsersApiMockServer.stubGetUserGroups(userId, true)

      webTestClient.get().uri("/users/$userId/groups")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
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
          """.trimIndent()
        )
    }

    @Test
    fun ` get user groups without children`() {
      externalUsersApiMockServer.stubGetUserGroups(userId, false)

      webTestClient.get().uri("/users/$userId/groups?children=false")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
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
          """.trimIndent()
        )
    }
  }

  @Nested
  inner class RemoveUserGroup {

    private val userId = UUID.fromString("7112EC3B-88C1-48C3-BCC3-F82874E3F2C3")
    private val group = "SITE_3_GROUP_1"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/users/$userId/groups/$group")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.delete().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `fail bad request`() {
      externalUsersApiMockServer.stubDeleteUserGroupFail(userId.toString(), group, BAD_REQUEST)
      webTestClient.delete().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(BAD_REQUEST.value())
          assertThat(it["userMessage"] as String).startsWith("User error message")
          assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }

    @Test
    fun `fail forbidden`() {
      externalUsersApiMockServer.stubDeleteUserGroupFail(userId.toString(), group, FORBIDDEN)
      webTestClient.delete().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(FORBIDDEN.value())
          assertThat(it["userMessage"] as String).startsWith("User error message")
          assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }

    @Test
    fun `success with role maintain oauth users`() {
      externalUsersApiMockServer.stubDeleteGroupFromUser(userId.toString(), group)
      webTestClient.delete().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `success with role auth group manager`() {
      externalUsersApiMockServer.stubDeleteGroupFromUser(userId.toString(), group)
      webTestClient.delete().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  inner class AddUserGroup {
    private val userId = UUID.fromString("7CA04ED7-8275-45B2-AFB4-4FF51432D1EC")
    private val group = "site_1_group_2"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/users/$userId/groups/$group")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.put().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `adds a group to a user`() {
      externalUsersApiMockServer.stubAddGroupToUser(userId.toString(), group)
      webTestClient
        .put().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `fail bad request`() {
      externalUsersApiMockServer.stubAddUserGroupFail(userId.toString(), group, BAD_REQUEST)
      webTestClient.put().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(BAD_REQUEST.value())
          assertThat(it["userMessage"] as String).startsWith("User error message")
          assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }

    @Test
    fun `fail forbidden`() {
      externalUsersApiMockServer.stubAddUserGroupFail(userId.toString(), group, FORBIDDEN)
      webTestClient.put().uri("/users/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(FORBIDDEN.value())
          assertThat(it["userMessage"] as String).startsWith("User error message")
          assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }
  }
}

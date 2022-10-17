package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import java.util.UUID

class UserGroupControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class RemoveUserGroup {

    private val userId = UUID.fromString("7112EC3B-88C1-48C3-BCC3-F82874E3F2C3")
    private val group = "SITE_3_GROUP_1"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/users/id/$userId/groups/$group")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/users/id/$userId/groups/$group")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.delete().uri("/users/id/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `fail bad request`() {
      externalUsersApiMockServer.stubDeleteUserGroupFail(userId.toString(), group, BAD_REQUEST)
      webTestClient.delete().uri("/users/id/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it["status"] as Int).isEqualTo(BAD_REQUEST.value())
          Assertions.assertThat(it["userMessage"] as String).startsWith("User error message")
          Assertions.assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }

    @Test
    fun `fail forbidden`() {
      externalUsersApiMockServer.stubDeleteUserGroupFail(userId.toString(), group, FORBIDDEN)
      webTestClient.delete().uri("/users/id/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it["status"] as Int).isEqualTo(FORBIDDEN.value())
          Assertions.assertThat(it["userMessage"] as String).startsWith("User error message")
          Assertions.assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }

    @Test
    fun `success with role maintain oauth users`() {
      externalUsersApiMockServer.stubDeleteGroupFromUser(userId.toString(), group)
      webTestClient.delete().uri("/users/id/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `success with role auth group manager`() {
      externalUsersApiMockServer.stubDeleteGroupFromUser(userId.toString(), group)
      webTestClient.delete().uri("/users/id/$userId/groups/$group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNoContent
    }
  }
}

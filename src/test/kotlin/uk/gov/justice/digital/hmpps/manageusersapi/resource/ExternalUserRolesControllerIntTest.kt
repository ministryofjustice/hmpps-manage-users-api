package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import java.util.UUID

class ExternalUserRolesControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class GetUserRoles {
    @Test
    fun `get user roles when no authority`() {
      webTestClient.get().uri("/externalusers/12345678-1234-5678-90ab-1234567890ab/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `get user roles forbidden when no role`() {

      webTestClient.get().uri("/externalusers/12345678-1234-5678-90ab-1234567890ab/roles")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get user roles forbidden when wrong role`() {

      webTestClient.get().uri("/externalusers/12345678-1234-5678-90ab-1234567890ab/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get user roles`() {
      val userId = UUID.randomUUID()
      externalUsersApiMockServer.stubGetUserRoles(userId)

      webTestClient.get().uri("/externalusers/$userId/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].roleName").isEqualTo("Audit viewer")
        .jsonPath("$[1].roleName")
        .isEqualTo("Auth Group Manager that has more than 30 characters in the role name")
    }
  }

  @Nested
  inner class RemoveUserRole {

    private val userId = UUID.randomUUID()
    private val role = "ROLE_TO_REMOVE"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/externalusers/$userId/roles/$role")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/externalusers/$userId/roles/$role")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.delete().uri("/externalusers/$userId/roles/$role")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `fail bad request`() {
      externalUsersApiMockServer.stubDeleteUserRoleFail(userId.toString(), role, HttpStatus.BAD_REQUEST)
      webTestClient.delete().uri("/externalusers/$userId/roles/$role")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it["status"] as Int).isEqualTo(HttpStatus.BAD_REQUEST.value())
          Assertions.assertThat(it["userMessage"] as String).startsWith("User error message")
          Assertions.assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }

    @Test
    fun `fail forbidden`() {
      externalUsersApiMockServer.stubDeleteUserRoleFail(userId.toString(), role, HttpStatus.FORBIDDEN)
      webTestClient.delete().uri("/externalusers/$userId/roles/$role")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it["status"] as Int).isEqualTo(HttpStatus.FORBIDDEN.value())
          Assertions.assertThat(it["userMessage"] as String).startsWith("User error message")
          Assertions.assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }

    @Test
    fun `success with role maintain oauth users`() {
      externalUsersApiMockServer.stubDeleteRoleFromUser(userId.toString(), role)
      webTestClient.delete().uri("/externalusers/$userId/roles/$role")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `success with role auth role manager`() {
      externalUsersApiMockServer.stubDeleteRoleFromUser(userId.toString(), role)
      webTestClient.delete().uri("/externalusers/$userId/roles/$role")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  inner class GetAssignableRoles {
    @Test
    fun `get assignable user roles when no token`() {
      webTestClient.get().uri("/externalusers/12345678-1234-5678-90ab-1234567890ab/assignable-roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `get assignable user roles forbidden when no role`() {

      webTestClient.get().uri("/externalusers/12345678-1234-5678-90ab-1234567890ab/assignable-roles")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get assignable user roles forbidden when wrong role`() {

      webTestClient.get().uri("/externalusers/12345678-1234-5678-90ab-1234567890ab/assignable-roles")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get assignable user roles`() {
      val userId = UUID.randomUUID()
      externalUsersApiMockServer.stubGetAssignableRoles(userId)

      webTestClient.get().uri("/externalusers/$userId/assignable-roles")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].roleName").isEqualTo("Audit viewer")
        .jsonPath("$[1].roleName")
        .isEqualTo("Auth Group Manager role")
    }
  }
}

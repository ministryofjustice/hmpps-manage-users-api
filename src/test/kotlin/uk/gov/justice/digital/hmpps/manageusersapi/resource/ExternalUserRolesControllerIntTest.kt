package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
}

package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class UserRolesControllerIntTest : IntegrationTestBase() {

  @Test
  fun `get user roles when no authority`() {
    webTestClient.get().uri("users/bob/roles")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `get user roles forbidden when no role`() {

    webTestClient.get().uri("users/bob/roles")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `get user roles forbidden when wrong role`() {

    webTestClient.get().uri("users/bob/roles")
      .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun ` get user roles - long role name added from hmpps-auth `() {
    hmppsAuthMockServer.stubGetRolesForRoleName()
    nomisApiMockServer.stubGetUserRoles("BOB")

    webTestClient.get().uri("users/BOB/roles")
      .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.dpsRoles[0].name").isEqualTo("Audit viewer")
      .jsonPath("$.dpsRoles.[1].name").isEqualTo("Auth Group Manager that has more than 30 characters in the role name")
  }
}

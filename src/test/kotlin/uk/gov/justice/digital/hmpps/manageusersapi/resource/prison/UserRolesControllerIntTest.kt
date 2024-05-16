package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class UserRolesControllerIntTest : IntegrationTestBase() {
  @Nested
  inner class GetUserRoles {

    @Test
    fun `get user roles when no authority`() {
      webTestClient.get().uri("/prisonusers/bob/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `get user roles forbidden when no role`() {
      webTestClient.get().uri("/prisonusers/bob/roles")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get user roles forbidden when wrong role`() {
      webTestClient.get().uri("/prisonusers/bob/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun ` get user roles - long role name added from hmpps-external-users `() {
      externalUsersApiMockServer.stubGetRolesForRoleName()
      nomisApiMockServer.stubGetUserRoles("BOB")

      webTestClient.get().uri("/prisonusers/BOB/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.dpsRoles[0].name").isEqualTo("Audit viewer")
        .jsonPath("$.dpsRoles[1].name")
        .isEqualTo("Auth Group Manager that has more than 30 characters in the role name")
    }
  }

  @Nested
  inner class AddUserRoles {
    @Test
    fun `post user roles when no authority`() {
      webTestClient.post().uri("/prisonusers/bob/roles")
        .body(fromValue(listOf("ROLE1", "ROLE2")))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `post user roles forbidden when no role`() {
      webTestClient.post().uri("/prisonusers/bob/roles")
        .headers(setAuthorisation(roles = listOf()))
        .body(fromValue(listOf("ROLE1", "ROLE2")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `post user roles forbidden when wrong role`() {
      webTestClient.post().uri("/prisonusers/bob/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .body(fromValue(listOf("ROLE1", "ROLE2")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `post user roles calls nomis api`() {
      nomisApiMockServer.stubPostUserRoles("BOB", "[\"ROLE1\",\"ROLE2\"]")

      webTestClient.post().uri("/prisonusers/BOB/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .body(fromValue(listOf("ROLE1", "ROLE2")))
        .exchange()
        .expectStatus().isCreated

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/BOB/roles?caseloadId=NWEB"))
          .withRequestBody(equalToJson("[\"ROLE1\",\"ROLE2\"]")),
      )
    }
  }

  @Nested
  inner class RemoveUserRole {
    @Test
    fun `remove user role when no authority`() {
      webTestClient.delete().uri("/prisonusers/bob/roles/ROLE1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `remove user roles forbidden when no role`() {
      webTestClient.delete().uri("/prisonusers/bob/roles/ROLE1")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `remove user role forbidden when caller presents wrong role`() {
      webTestClient.delete().uri("/prisonusers/bob/roles/ROLE1")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `remove user roles calls nomis api`() {
      nomisApiMockServer.stubDeleteUserRole("BOB", "ROLE1")

      webTestClient.delete().uri("/prisonusers/BOB/roles/ROLE1")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(deleteRequestedFor(urlEqualTo("/users/BOB/roles/ROLE1?caseloadId=NWEB")))
    }
  }
}

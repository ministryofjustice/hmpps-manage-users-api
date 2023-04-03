package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class SyncControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class SyncUserEmail {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/prisonusers/SYNC_ME/email/sync")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/prisonusers/SYNC_ME/email/sync")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Can request email sync`() {
      nomisApiMockServer.stubFindUserByUsername("SYNC_ME")
      hmppsAuthMockServer.stubSyncNomisEmail("SYNC_ME")
      webTestClient
        .post().uri("/prisonusers/SYNC_ME/email/sync")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
      hmppsAuthMockServer.verify(postRequestedFor(urlEqualTo("/auth/api/prisonuser/SYNC_ME/email/sync")))
    }

    @Test
    fun `Can request email sync as local admin`() {
      nomisApiMockServer.stubFindUserByUsername("SYNC_ME_1")
      hmppsAuthMockServer.stubSyncNomisEmail("SYNC_ME_1")
      webTestClient
        .post().uri("/prisonusers/SYNC_ME_1/email/sync")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES")))
        .exchange()
        .expectStatus().isOk
      hmppsAuthMockServer.verify(postRequestedFor(urlEqualTo("/auth/api/prisonuser/SYNC_ME_1/email/sync")))
    }

    @Test
    fun `Will fail gracefully matching auth error message if no nomis user`() {
      nomisApiMockServer.stubGetFail("/users/SYNC_ME_2", HttpStatus.NOT_FOUND)
      webTestClient
        .post().uri("/prisonusers/SYNC_ME_2/email/sync")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          Assertions.assertThat(it["error"]).isEqualTo("Not Found")
          Assertions.assertThat(it["error_description"]).isEqualTo("Account for username SYNC_ME_2 not found")
          Assertions.assertThat(it["field"]).isEqualTo("username")
        }
      hmppsAuthMockServer.verify(0, postRequestedFor(urlEqualTo("/auth/api/prisonuser/SYNC_ME_2/email/sync")))
    }

    @Test
    fun `Will return ok if no email for nomis user`() {
      nomisApiMockServer.stubFindUserByUsernameNoEmail("SYNC_ME_3")
      webTestClient
        .post().uri("/prisonusers/SYNC_ME_3/email/sync")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
      hmppsAuthMockServer.verify(0, postRequestedFor(urlEqualTo("/auth/api/prisonuser/SYNC_ME_3/email/sync")))
    }
  }
}

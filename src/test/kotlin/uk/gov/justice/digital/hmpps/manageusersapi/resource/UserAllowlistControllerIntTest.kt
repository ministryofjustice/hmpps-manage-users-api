package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class UserAllowlistControllerIntTest : IntegrationTestBase() {
  @Nested
  inner class AddUser {
    @Test
    fun `Add user to allow list is created`() {
      hmppsAuthMockServer.stubPostWithStatus("/auth/api/user/allowlist", HttpStatus.CREATED)
      val allowListAddRequest = UserAllowlistAddRequest(
        "LAMISHACT",
        "consuella.tapscott@justice.gov.uk",
        "Consuella",
        "Tapscott",
        "testing",
        AccessPeriod.ONE_MONTH,
      )

      webTestClient
        .post().uri("/users/allowlist")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST", listOf("ROLE_MANAGE_USER_ALLOW_LIST")))
        .bodyValue(allowListAddRequest)
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `Add user to allow list is conflict if trying to add same username twice`() {
      hmppsAuthMockServer.stubPostWithStatus("/auth/api/user/allowlist", HttpStatus.CONFLICT)
      val allowListAddRequest = UserAllowlistAddRequest(
        "LAMISHACT",
        "consuella.tapscott@justice.gov.uk",
        "Consuella",
        "Tapscott",
        "testing",
        AccessPeriod.THREE_MONTHS,
      )

      webTestClient
        .post().uri("/users/allowlist")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST", listOf("ROLE_MANAGE_USER_ALLOW_LIST")))
        .bodyValue(allowListAddRequest)
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    fun `Add user to allow list is forbidden if missing role`() {
      val allowListAddRequest = UserAllowlistAddRequest(
        "LAMISHACT",
        "consuella.tapscott@justice.gov.uk",
        "Consuella",
        "Tapscott",
        "testing",
        AccessPeriod.SIX_MONTHS,
      )

      webTestClient
        .post().uri("/users/allowlist")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST"))
        .bodyValue(allowListAddRequest)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Add user to allow list is unauthorized if not authenticated`() {
      val allowListAddRequest = UserAllowlistAddRequest(
        "LAMISHACT",
        "consuella.tapscott@justice.gov.uk",
        "Consuella",
        "Tapscott",
        "testing",
        AccessPeriod.TWELVE_MONTHS,
      )

      webTestClient
        .post().uri("/users/allowlist")
        .bodyValue(allowListAddRequest)
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class UpdateUser {
    @Test
    fun `Update users allow list access is ok`() {
      val authAdmId = "5105a589-75b3-4ca0-9433-b96228c1c8f3"
      hmppsAuthMockServer.stubUpdateAllowlistUserWithStatus(authAdmId)
      val updateRequest = UserAllowlistPatchRequest(
        "testing",
        AccessPeriod.NO_RESTRICTION,
      )

      webTestClient
        .patch().uri("/users/allowlist/$authAdmId")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST", listOf("ROLE_MANAGE_USER_ALLOW_LIST")))
        .bodyValue(updateRequest)
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Update users allow list access is not found if id does not exist`() {
      val unknownId = "2268a29c-3a50-43fd-a8a3-68a6f3a41226"
      hmppsAuthMockServer.stubUpdateAllowlistUserWithStatus(unknownId, HttpStatus.NOT_FOUND)
      val updateRequest = UserAllowlistPatchRequest(
        "testing",
        AccessPeriod.EXPIRE,
      )

      webTestClient
        .patch().uri("/users/allowlist/$unknownId")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST", listOf("ROLE_MANAGE_USER_ALLOW_LIST")))
        .bodyValue(updateRequest)
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `Update users allow list access is forbidden if missing role`() {
      val authAdmId = "5105a589-75b3-4ca0-9433-b96228c1c8f3"
      val updateRequest = UserAllowlistPatchRequest(
        "testing",
        AccessPeriod.SIX_MONTHS,
      )

      webTestClient
        .patch().uri("/users/allowlist/$authAdmId")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST"))
        .bodyValue(updateRequest)
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Update users allow list access is unauthorized if not authenticated`() {
      val authAdmId = "5105a589-75b3-4ca0-9433-b96228c1c8f3"
      val updateRequest = UserAllowlistPatchRequest(
        "testing",
        AccessPeriod.SIX_MONTHS,
      )

      webTestClient
        .patch().uri("/users/allowlist/$authAdmId")
        .bodyValue(updateRequest)
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class GetUser {
    @Test
    fun `Get user is ok`() {
      hmppsAuthMockServer.stubGetAllowlistUserWithStatus("AUTH_ADM")
      webTestClient
        .get().uri("/users/allowlist/AUTH_ADM")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST", listOf("ROLE_MANAGE_USER_ALLOW_LIST")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Get user is not found if username does not exist`() {
      hmppsAuthMockServer.stubGetAllowlistUserWithStatus("UNKNOWN_ALLOW_LIST_USER", HttpStatus.NOT_FOUND)
      webTestClient
        .get().uri("/users/allowlist/UNKNOWN_ALLOW_LIST_USER")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST", listOf("ROLE_MANAGE_USER_ALLOW_LIST")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `Get user is forbidden if missing role`() {
      webTestClient
        .get().uri("/users/allowlist/AUTH_ADM")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Get user is unauthorized if not authenticated`() {
      webTestClient
        .get().uri("/users/allowlist/AUTH_ADM")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class GetUsers {
    @Test
    fun `Get users is ok default filter`() {
      hmppsAuthMockServer.stubGetAllAllowlistUserWithStatus()
      webTestClient
        .get().uri("/users/allowlist")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST", listOf("ROLE_MANAGE_USER_ALLOW_LIST")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content.length()").isEqualTo(1)
        .jsonPath("$.size").isEqualTo(10)
        .jsonPath("$.totalElements").isEqualTo(1)
        .jsonPath("$.totalPages").isEqualTo(1)
    }

    @Test
    fun `Get users is ok filtered by name`() {
      hmppsAuthMockServer.stubGetAllAllowlistUserWithStatus("?name=AUTH&status=ALL")
      webTestClient
        .get().uri("/users/allowlist?name=AUTH")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST", listOf("ROLE_MANAGE_USER_ALLOW_LIST")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content.length()").isEqualTo(1)
        .jsonPath("$.size").isEqualTo(10)
        .jsonPath("$.totalElements").isEqualTo(1)
        .jsonPath("$.totalPages").isEqualTo(1)
    }

    @Test
    fun `Get users is ok filtered by status`() {
      hmppsAuthMockServer.stubGetAllAllowlistUserWithStatus("?status=EXPIRED")
      webTestClient
        .get().uri("/users/allowlist?status=EXPIRED")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST", listOf("ROLE_MANAGE_USER_ALLOW_LIST")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content.length()").isEqualTo(1)
        .jsonPath("$.size").isEqualTo(10)
        .jsonPath("$.totalElements").isEqualTo(1)
        .jsonPath("$.totalPages").isEqualTo(1)
    }

    @Test
    fun `Get users is forbidden if missing role`() {
      webTestClient
        .get().uri("/users/allowlist")
        .headers(setAuthorisation("AUTH_MANAGE_USER_ALLOW_LIST"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Get users is unauthorized if not authenticated`() {
      webTestClient
        .get().uri("/users/allowlist")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }
}

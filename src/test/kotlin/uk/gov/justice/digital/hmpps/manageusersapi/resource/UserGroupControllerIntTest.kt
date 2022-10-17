package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import java.util.UUID
import java.util.UUID.fromString

class UserGroupControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class GetUserGroups {
    private val userId: UUID = fromString("5E3850B9-9D6E-49D7-B8E7-42874D6CEEA8")
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
      externalUsersApiMockServer.stubGetNotFound("/users/id/$userId/groups?children=true")
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
}

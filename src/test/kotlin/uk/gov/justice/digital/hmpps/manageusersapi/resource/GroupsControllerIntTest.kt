package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class GroupsControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class GroupDetails {

    @AfterEach
    fun resetMocks() {
      externalUsersApiMockServer.resetAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/groups/SITE_1_GROUP_2")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Group details endpoint returns details of group when user has ROLE_MAINTAIN_OAUTH_USERS`() {
      externalUsersApiMockServer.stubGetGroupDetails("SITE_1_GROUP_2")
      webTestClient
        .get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .json(
          """
      {
                    "groupCode": "SITE_1_GROUP_2",
                    "groupName": "Site 1 - Group 2",
                    "assignableRoles": [
                      {
                        "roleCode": "GLOBAL_SEARCH",
                        "roleName": "Global Search"
                      },
                      {
                        "roleCode": "LICENCE_RO",
                        "roleName": "Licence Responsible Officer"
                      }
                    ],
                    "children": [
                      {
                        "groupCode": "CHILD_1",
                        "groupName": "Child - Site 1 - Group 2"
                      }
                    ]
                  }  
          """.trimIndent()
        )
    }
    @Test
    fun `Group details endpoint returns details of group when user is able to maintain group`() {
      externalUsersApiMockServer.stubGetGroupDetails("SITE_1_GROUP_2")
      webTestClient
        .get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_GROUP_MANAGER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .json(
          """
      {
                    "groupCode": "SITE_1_GROUP_2",
                    "groupName": "Site 1 - Group 2",
                    "assignableRoles": [
                      {
                        "roleCode": "GLOBAL_SEARCH",
                        "roleName": "Global Search"
                      },
                      {
                        "roleCode": "LICENCE_RO",
                        "roleName": "Licence Responsible Officer"
                      }
                    ],
                    "children": [
                      {
                        "groupCode": "CHILD_1",
                        "groupName": "Child - Site 1 - Group 2"
                      }
                    ]
                  }  
          """.trimIndent()
        )
    }

    @Test
    fun `Group details endpoint returns error when user is not allowed to maintain group`() {
      externalUsersApiMockServer.stubGetGroupDetailsForUserNotNotAllowed("SITE_1_GROUP_2", FORBIDDEN)
      webTestClient
        .get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isEqualTo(FORBIDDEN)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to FORBIDDEN.value(),
              "developerMessage" to "Unable to maintain group: SITE_1_GROUP_2 with reason: Group not with your groups",
              "userMessage" to "Auth maintain group relationship exception: Unable to maintain group: SITE_1_GROUP_2 with reason: Group not with your groups",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }
  }

  @Nested
  inner class ChangeChildGroupName {
    @Test
    fun `Change group name`() {
      externalUsersApiMockServer.stubPutUpdateChildGroup("CHILD_9")
      webTestClient
        .put().uri("/groups/child/CHILD_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change group name returns error when group not found`() {
      externalUsersApiMockServer.stubPutUpdateChildGroupFail("Not_A_Group", NOT_FOUND)
      webTestClient
        .put().uri("/groups/child/Not_A_Group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(BodyInserters.fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(NOT_FOUND.value())
          assertThat(it["userMessage"] as String).startsWith("Group Not found: Unable to maintain group: Not_A_Group with reason: notfound")
          assertThat(it["developerMessage"] as String).startsWith("Unable to maintain group: Not_A_Group with reason: notfound")
        }
    }

    @Test
    fun `Group details endpoint not accessible without valid token`() {
      webTestClient.put().uri("/groups/child/CHILD_9")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }
}

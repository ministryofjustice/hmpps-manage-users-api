package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters.fromValue
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
        .expectHeader().contentType(APPLICATION_JSON)
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
        .expectHeader().contentType(APPLICATION_JSON)
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
      externalUsersApiMockServer.stubGetGroupDetailsForUserNotAllowed("SITE_1_GROUP_2", FORBIDDEN)
      webTestClient
        .get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isEqualTo(FORBIDDEN)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to FORBIDDEN.value(),
              "developerMessage" to "Developer message",
              "userMessage" to "User message",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }

    @Test
    fun `Group details endpoint returns error when group in not found`() {
      externalUsersApiMockServer.stubCreateGroupNotFound("SITE_1_GROUP_2")
      webTestClient
        .get().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isEqualTo(NOT_FOUND)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to NOT_FOUND.value(),
              "developerMessage" to "Unable to get group: SITE_1_GROUP_2 with reason: notfound",
              "userMessage" to "Group Not found: Unable to get group: SITE_1_GROUP_2 with reason: notfound",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }
  }

  @Nested
  inner class ChildGroupDetails {

    @AfterEach
    fun resetMocks() {
      externalUsersApiMockServer.resetAll()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/groups/child/CHILD_1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/groups/child/CHILD_1")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `returns details of child group when user has ROLE_MAINTAIN_OAUTH_USERS`() {
      externalUsersApiMockServer.stubGetChildGroupDetails("CHILD_1")
      webTestClient
        .get().uri("/groups/child/CHILD_1")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .json(
          """
      {
                    "groupCode": "CHILD_1",
                    "groupName": "Child - Site 1 - Group 2"
                  }  
          """.trimIndent()
        )
    }

    @Test
    fun `returns error when user not allowed to maintain group`() {
      externalUsersApiMockServer.stubGetChildGroupDetailsForUserNotAllowed("CHILD_1", FORBIDDEN)
      webTestClient
        .get().uri("/groups/child/CHILD_1")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isEqualTo(FORBIDDEN)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to FORBIDDEN.value(),
              "developerMessage" to "Access is denied",
              "userMessage" to "Access is denied",
            )
          )
        }
    }

    @Test
    fun `returns error when child group not found`() {
      externalUsersApiMockServer.stubNotFound("/groups/child/CHILD_1")
      webTestClient
        .get().uri("/groups/child/CHILD_1")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isEqualTo(NOT_FOUND)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to NOT_FOUND.value(),
              "developerMessage" to "Developer message",
              "userMessage" to "User message",
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
        .body(fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Invalid group name`() {
      webTestClient
        .put().uri("/groups/child/CHILD_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(fromValue(mapOf("groupName" to "new")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).contains("default message [groupName]")
          assertThat(it["userMessage"] as String).contains("default message [size must be between 4 and 100]")
          assertThat(it["developerMessage"] as String).contains("default message [groupName]")
          assertThat(it["developerMessage"] as String).contains("default message [size must be between 4 and 100]")
        }
    }

    @Test
    fun `Change group name returns error when group not found`() {
      externalUsersApiMockServer.stubPutUpdateChildGroupFail("Not_A_Group", NOT_FOUND)
      webTestClient
        .put().uri("/groups/child/Not_A_Group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(NOT_FOUND.value())
          assertThat(it["userMessage"] as String).startsWith("Group Not found: Unable to maintain group: Not_A_Group with reason: notfound")
          assertThat(it["developerMessage"] as String).startsWith("Unable to maintain group: Not_A_Group with reason: notfound")
        }
    }
    @Test
    fun `Group details endpoint returns error when group in not found`() {
      externalUsersApiMockServer.stubUpdateChildGroupNotFound("CHILD_9")

      webTestClient
        .put().uri("/groups/child/CHILD_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isEqualTo(NOT_FOUND)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to NOT_FOUND.value(),
              "developerMessage" to "Unable to get group: CHILD_9 with reason: notfound",
              "userMessage" to "Child Group Not found: Unable to get group: CHILD_9 with reason: notfound",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }
    @Test
    fun `Group details endpoint not accessible without valid token`() {
      webTestClient.put().uri("/groups/child/CHILD_9")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class ChangeGroupName {
    @Test
    fun `Change group name`() {
      externalUsersApiMockServer.stubPutUpdateGroup("GROUP_9")
      webTestClient
        .put().uri("/groups/GROUP_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Invalid group name`() {
      webTestClient
        .put().uri("/groups/GROUP_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(fromValue(mapOf("groupName" to "new")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).contains("default message [groupName]")
          assertThat(it["userMessage"] as String).contains("default message [size must be between 4 and 100]")
          assertThat(it["developerMessage"] as String).contains("default message [groupName]")
          assertThat(it["developerMessage"] as String).contains("default message [size must be between 4 and 100]")
        }
    }

    @Test
    fun `Change group name returns error when group not found`() {
      externalUsersApiMockServer.stubPutUpdateGroupFail("Not_A_Group", NOT_FOUND)
      webTestClient
        .put().uri("/groups/Not_A_Group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(NOT_FOUND.value())
          assertThat(it["userMessage"] as String).startsWith("User error message")
          assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/groups/GROUP_9")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/groups/GROUP_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf()))
        .body(fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.put().uri("/groups/GROUP_9")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_AUDIT")))
        .body(fromValue(mapOf("groupName" to "new group name")))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  @Nested
  inner class CreateGroup {
    @Test
    fun `Create group`() {
      externalUsersApiMockServer.stubCreateGroup()
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          fromValue(
            mapOf(
              "groupCode" to "CG",
              "groupName" to " groupie"
            )
          )
        )
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Create group error`() {

      externalUsersApiMockServer.stubCreateGroupFail(BAD_REQUEST)
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          fromValue(
            mapOf(
              "groupCode" to "x", "groupName" to "123",
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).contains("default message [groupName],100,4]")
          assertThat(it["userMessage"] as String).contains("default message [groupCode],30,2]")
        }
    }

    @Test
    fun `Create group endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("bob"))
        .body(
          fromValue(
            mapOf(
              "groupCode" to "CG3",
              "groupName" to " groupie 3"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
      {"userMessage":"Access is denied","developerMessage":"Access is denied"}
          """.trimIndent()
        )
    }

    @Test
    fun `Create group - group already exists`() {

      externalUsersApiMockServer.stubCreateGroupsConflict()

      webTestClient
        .post().uri("/groups")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          fromValue(
            mapOf(
              "groupCode" to "CG1",
              "groupName" to " groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to CONFLICT.value(),
              "errorCode" to null,
              "moreInfo" to null,
              "userMessage" to "User test message",
              "developerMessage" to "Developer test message"
            )
          )
        }
    }

    @Test
    fun `Create group endpoint not accessible without valid token`() {
      webTestClient.post().uri("/groups")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class CreateChildGroup {
    @Test
    fun `Create child group`() {

      externalUsersApiMockServer.stubCreateChildGroup()
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG",
              "groupName" to "Child groupie"
            )
          )
        )
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Create child group error`() {
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          fromValue(
            mapOf(
              "parentGroupCode" to "",
              "groupCode" to "",
              "groupName" to ""
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `Create child group endpoint returns forbidden when does not have admin role`() {
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("bob"))
        .body(
          fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG3",
              "groupName" to "Child groupie 3"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
     {"userMessage":"Access is denied","developerMessage":"Access is denied"}
          """.trimIndent()
        )
    }

    @Test
    fun `Create Child group length too short`() {

      externalUsersApiMockServer.stubCreateChildrenGroupFail(BAD_REQUEST)
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          fromValue(
            mapOf(
              "parentGroupCode" to "",
              "groupCode" to "",
              "groupName" to ""
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).contains("default message [groupCode],30,2]")
          assertThat(it["userMessage"] as String).contains("default message [groupName],100,4]")
          assertThat(it["userMessage"] as String).contains("default message [parentGroupCode],30,2]")
        }
    }

    @Test
    fun `Create child group - group already exists`() {
      externalUsersApiMockServer.stubCreateChildGroupsConflict()
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          fromValue(
            mapOf(
              "parentGroupCode" to "SITE_9_GROUP_1",
              "groupCode" to "CG1",
              "groupName" to "Child groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "developerMessage" to "Developer test message",
              "userMessage" to "User test message",
              "errorCode" to null,
              "moreInfo" to null,
              "status" to CONFLICT.value()
            )
          )
        }
    }

    @Test
    fun `Create child group - parent group doesnt exist`() {
      externalUsersApiMockServer.stubCreateChildGroupNotFound()
      webTestClient
        .post().uri("/groups/child")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .body(
          fromValue(
            mapOf(
              "parentGroupCode" to "pg",
              "groupCode" to "CG1",
              "groupName" to "Child groupie 1"
            )
          )
        )
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "developerMessage" to "Unable to create group: PG with reason: ParentGroupNotFound",
              "userMessage" to "Group Not found: Unable to create group: PG with reason: ParentGroupNotFound",
              "errorCode" to null,
              "moreInfo" to null,
              "status" to NOT_FOUND.value()
            )
          )
        }
    }

    @Test
    fun `Create Child Group endpoint not accessible without valid token`() {
      webTestClient.post().uri("/groups/child")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  inner class DeleteChildGroup {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/groups/child/CHILD_3")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/groups/child/CHILD_3")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.delete().uri("/groups/child/CHILD_3")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `child group not found`() {
      externalUsersApiMockServer.stubDeleteChildGroupFail("Not_A_Group", NOT_FOUND)
      webTestClient
        .delete().uri("/groups/child/Not_A_Group")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(NOT_FOUND.value())
          assertThat(it["userMessage"] as String).startsWith("User error message")
          assertThat(it["developerMessage"] as String).startsWith("Developer error message")
        }
    }

    @Test
    fun `delete child group success`() {
      externalUsersApiMockServer.stubDeleteChildGroup("CHILD_3")
      webTestClient
        .delete().uri("/groups/child/CHILD_3")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  inner class DeleteGroup {
    @Test
    fun `Delete Group - no child groups and no members`() {

      externalUsersApiMockServer.stubDeleteGroup()
      webTestClient.delete().uri("/groups/GC_DEL_1")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Delete Group - has child groups`() {
      externalUsersApiMockServer.stubDeleteGroupsConflict()
      webTestClient.delete().uri("/groups/GC_DEL_3")
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to CONFLICT.value(),
              "errorCode" to null,
              "moreInfo" to null,
              "userMessage" to "Unable to delete group: GC_DEL_3 with reason: child group exist",
              "developerMessage" to "Developer test message"
            )
          )
        }
    }
    @Test
    fun `Delete Group endpoint returns forbidden when does not have admin role`() {
      webTestClient.delete().uri("/groups/GC_DEL_1")
        .headers(setAuthorisation("bob"))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
      {"userMessage":"Access is denied","developerMessage":"Access is denied"}
          """.trimIndent()
        )
    }
    @Test
    fun `Delete Group details endpoint not accessible without valid token`() {
      webTestClient.delete().uri("/groups/GC_DEL_1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Delete Group returns error when group not found`() {
      externalUsersApiMockServer.stubDeleteGroupNotFound("SITE_1_GROUP_2")
      webTestClient
        .delete().uri("/groups/SITE_1_GROUP_2")
        .headers(setAuthorisation("AUTH_USER", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isEqualTo(NOT_FOUND)
        .expectHeader().contentType(APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "status" to NOT_FOUND.value(),
              "developerMessage" to "Unable to delete group: SITE_1_GROUP_2 with reason: notfound",
              "userMessage" to "Group Not found: Unable to delete group: SITE_1_GROUP_2 with reason: notfound",
              "errorCode" to null,
              "moreInfo" to null
            )
          )
        }
    }
  }
}

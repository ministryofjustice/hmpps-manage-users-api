package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.hasItems
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class RolesControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class CreateRole {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf()))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create role`() {
      hmppsAuthMockServer.stubCreateRole()
      nomisApiMockServer.stubCreateRole()

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create role - role already exists`() {
      hmppsAuthMockServer.stubCreateRoleFail(409)
      nomisApiMockServer.stubCreateRole()

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Unexpected error: Unable to create role: RC1 with reason: role code already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("Unable to create role: RC1 with reason: role code already exists")
        }
    }

    @Test
    fun `create role returns error when role code length too short`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "R",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Role code must be between 2 and 30 characters")
        )
    }

    @Test
    fun `create role returns error when role code length too long`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "R".repeat(30) + "y",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Role code must be between 2 and 30 characters")
        )
    }

    @Test
    fun `create role returns error when role code failed regex`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "R0L$%",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Role code must only contain 0-9, A-Z, a-z and _  characters")
        )
    }

    @Test
    fun `create role returns error when role name length too short`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "ROLE1",
              "roleName" to "R",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Role name must be between 4 and 100 characters")
        )
    }

    @Test
    fun `create role returns error when role name length too long`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "ROLE1",
              "roleName" to "R".repeat(128) + "y",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Role name must be between 4 and 100 characters")
        )
    }

    @Test
    fun `create role returns error when role name failed regex`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "ROLE1",
              "roleName" to "Role name $%",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Role name must only contain 0-9, A-Z, a-z and ( ) & , - . '  characters")
        )
    }

    @Test
    fun `create role returns error when role description length too long`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "ROLE1",
              "roleName" to "Role name",
              "roleDescription" to "D".repeat(1024) + "y",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Role description must be no more than 1024 characters")
        )
    }

    @Test
    fun `create role returns error when role description failed regex`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "ROLE1",
              "roleName" to "Role name",
              "roleDescription" to "Description <>%",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Role description must only contain can only contain 0-9, A-Z, a-z, newline and ( ) & , - . '  characters")
        )
    }

    @Test
    fun `create role returns error when admin type not present`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "roleCode" to "R0LE1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf<String>()
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Admin type cannot be empty")
        )
    }
  }

  @Nested
  inner class RoleDetails {

    @Test
    fun `access forbidden when no authority`() {

      webTestClient.get().uri("/roles/role-code")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {

      webTestClient.get().uri("/roles/AUTH_GROUP_MANAGER")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {

      webTestClient.get().uri("/roles/AUTH_GROUP_MANAGER")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get role`() {
      hmppsAuthMockServer.stubGetRolesDetails()
      webTestClient.get().uri("/roles/AUTH_GROUP_MANAGER")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          """
          {
          "roleCode":"AUTH_GROUP_MANAGER",
          "roleName":"Group Manager",
          "roleDescription":"Allow Group Manager to administer the account within their groups",
          "adminType":[
            {
            "adminTypeCode":"EXT_ADM",
            "adminTypeName":"External Administrator"}]
          }
          """
        )
    }
  }

  @Nested
  inner class AmendRoleName {

    @Test
    fun `Change role name endpoint not accessible without valid token`() {
      webTestClient.put().uri("/roles/ANY_ROLE")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Change role name endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .put().uri("/roles/ANY_ROLE")
        .headers(setAuthorisation(roles = listOf()))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          mapOf(
            "status" to "403"
          )
        }
    }

    @Test
    fun `Change role name returns error when role not found`() {
      hmppsAuthMockServer.stubPutRoleNameFail("Not_A_Role", 404)
      webTestClient
        .put().uri("/roles/Not_A_Role")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("404")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Unexpected error: Unable to get role: Not_A_Role with reason: notfound")
          assertThat(it["developerMessage"] as String).isEqualTo("Unable to get role: Not_A_Role with reason: notfound")
        }
    }

    @Test
    fun `Change role name returns error when length too short`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "tim")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("errors").value(
          hasItems("Role name must be between 4 and 100 characters")
        )
    }

    @Test
    fun `Change role name returns error when length too long`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "12345".repeat(20) + "y",)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("errors").value(
          hasItems("Role name must be between 4 and 100 characters")
        )
    }

    @Test
    fun `Change role name failed regex`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "a\$here")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("errors").value(
          hasItems("Role name must only contain 0-9, a-z and ( ) & , - . '  characters")
        )
    }

    @Test
    fun `Change role name success`() {
      hmppsAuthMockServer.stubPutRoleName("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role name passes regex validation`() {
      hmppsAuthMockServer.stubPutRoleName("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "good's & Role(),.-")))
        .exchange()
        .expectStatus().isOk
    }
  }

  @Nested
  inner class AmendRoleDescription {

    @Test
    fun `Change role description endpoint not accessible without valid token`() {
      webTestClient.put().uri("/roles/ANY_ROLE/description")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Change role description endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .put().uri("/roles/ANY_ROLE/description")
        .headers(setAuthorisation("bob"))
        .body(fromValue(mapOf("roleDescription" to "new role description")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          mapOf("status" to "403")
        }
    }

    @Test
    fun `Change role description returns error when role not found`() {
      hmppsAuthMockServer.stubPutRoleDescriptionFail("Not_A_Role", 404)
      webTestClient
        .put().uri("/roles/Not_A_Role/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "new role description")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Unexpected error: Unable to get role: Not_A_Role with reason: notfound")
          assertThat(it["developerMessage"] as String).isEqualTo("Unable to get role: Not_A_Role with reason: notfound")
        }
    }

    @Test
    fun `Change role description returns error when length too long`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "12345".repeat(205) + "y")))
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().jsonPath("errors").value(
          hasItems("Role description must be no more than 1024 characters")
        )
    }

    @Test
    fun `Change role description failed regex`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "a\$here")))
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("errors").value(
          hasItems("Role description must only contain can only contain 0-9, a-z, newline and ( ) & , - . '  characters")
        )
    }

    @Test
    fun `Change role description success`() {
      hmppsAuthMockServer.stubPutRoleDescription("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "new role description")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description returns success for empty roleDescription`() {
      hmppsAuthMockServer.stubPutRoleDescription("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description returns success for no role description`() {
      hmppsAuthMockServer.stubPutRoleDescription("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to null)))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description passes regex validation`() {
      hmppsAuthMockServer.stubPutRoleDescription("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "good's & Role(),.-lineone\r\nlinetwo")))
        .exchange()
        .expectStatus().isOk
    }
  }
}

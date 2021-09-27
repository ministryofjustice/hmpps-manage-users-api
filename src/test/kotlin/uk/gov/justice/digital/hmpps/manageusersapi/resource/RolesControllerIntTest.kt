package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.hasItems
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth

class RolesControllerIntTest : IntegrationTestBase() {

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
      hmppsAuth.stubGetRolesDetails()
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
      hmppsAuth.stubPutRoleNameFail("Not_A_Role", NOT_FOUND)
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
      hmppsAuth.stubPutRoleName("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role name passes regex validation`() {
      hmppsAuth.stubPutRoleName("OAUTH_ADMIN")
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
      hmppsAuth.stubPutRoleDescriptionFail("Not_A_Role", NOT_FOUND)
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
      hmppsAuth.stubPutRoleDescription("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "new role description")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description returns success for empty roleDescription`() {
      hmppsAuth.stubPutRoleDescription("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description returns success for no role description`() {
      hmppsAuth.stubPutRoleDescription("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to null)))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description passes regex validation`() {
      hmppsAuth.stubPutRoleDescription("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "good's & Role(),.-lineone\r\nlinetwo")))
        .exchange()
        .expectStatus().isOk
    }
  }


  @Nested
  inner class AmendRoleAdminType {

    @Test
    fun `Change role adminType endpoint not accessible without valid token`() {
      webTestClient.put().uri("/roles/ANY_ROLE/admintype")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Change role adminType endpoint returns forbidden when does not have admin role `() {
      webTestClient
        .put().uri("/roles/ANY_ROLE/admintype")
        .headers(setAuthorisation(roles = listOf()))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM"))))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          mapOf("status" to "403")
        }
    }

    @Test
    fun `Change role admin type returns error when role not found`() {
      hmppsAuth.stubPutRoleAdminTypeFail("Not_A_Role", NOT_FOUND)
      webTestClient
        .put().uri("/roles/Not_A_Role/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM"))))
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
    fun `Change role adminType returns bad request for no admin type`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf<String>())))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("errors").value(
          hasItems("Admin type cannot be empty")
        )
    }

    @Test
    fun `Change role admin type returns bad request when adminType does not exist`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DOES_NOT_EXIST"))))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `Change role admin type returns success`() {
      hmppsAuth.stubPutRoleAdminType("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM"))))
        .exchange()
        .expectStatus().isOk
    }
  }
}




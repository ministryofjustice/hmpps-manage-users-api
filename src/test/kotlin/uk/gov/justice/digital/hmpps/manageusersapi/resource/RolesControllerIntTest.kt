package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.hasItems
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
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
          fromValue(
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
          fromValue(
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
          fromValue(
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
    fun `create role ROLE_`() {
      hmppsAuthMockServer.stubCreateRole()
      nomisApiMockServer.stubCreateRole()

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "ROLE_RC2",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isCreated

      hmppsAuthMockServer.verify(
        postRequestedFor(urlEqualTo("/auth/api/roles"))
          .withRequestBody(
            containing("{\"roleCode\":\"RC2\",\"roleName\":\"new role name\",\"roleDescription\":\"Description\",\"adminType\":[\"EXT_ADM\"]}")
          )
      )
    }

    @Test
    fun `create role returns error when role exists`() {
      hmppsAuthMockServer.stubCreateRoleFail(CONFLICT)
      nomisApiMockServer.stubCreateRole()
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM")
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
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
          fromValue(
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
          fromValue(
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
          fromValue(
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
          fromValue(
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
          fromValue(
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
          fromValue(
            mapOf(
              "roleCode" to "R0LE1",
              "roleName" to "new role name$#",
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
          fromValue(
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
          fromValue(
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
          fromValue(
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

    @Test
    fun `create role returns error when admin type does not exist`() {
      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "R0LE1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("DOES_NOT_EXIST")
            )
          )
        )
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  inner class GetAllRoles {
    @Test
    fun `access forbidden when no authority`() {

      webTestClient.get().uri("/roles")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {

      webTestClient.get().uri("/roles")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {

      webTestClient.get().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get all roles defaults`() {
      hmppsAuthMockServer.stubGetRoles()
      webTestClient.get().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].roleName").isEqualTo("Audit viewer")
        .jsonPath("$[1].roleName").isEqualTo("Auth Group Manager")
        .jsonPath("$[2].roleName").isEqualTo("role 1")
        .jsonPath("$[3].roleName").isEqualTo("role 2")
        .jsonPath("$[4].roleName").isEqualTo("role 3")
        .jsonPath("$[5].roleName").isEqualTo("role 4")
        .jsonPath("$[6].roleName").isEqualTo("role 5")
        .jsonPath("$[7].roleName").isEqualTo("role 6")
        .jsonPath("$[8].roleName").isEqualTo("role 7")
        .jsonPath("$[9].roleName").isEqualTo("role 8")
    }

    @Test
    fun `get all roles filter admin type`() {
      hmppsAuthMockServer.stubGetAllRolesFilterAdminType()
      webTestClient.get().uri("/roles?adminTypes=EXT_ADM")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].adminType[0].adminTypeCode").isEqualTo("EXT_ADM")
    }

    @Test
    fun `get all roles filter multiple admin types`() {
      hmppsAuthMockServer.stubGetAllRolesFilterAdminTypes()
      webTestClient.get().uri("/roles?adminTypes=EXT_ADM&adminTypes=DPS_ADM")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].adminType[0].adminTypeCode").isEqualTo("EXT_ADM")
        .jsonPath("$[0].adminType[1].adminTypeCode").isEqualTo("DPS_ADM")
    }
  }

  @Nested
  inner class GetAllPagedRoles {
    @Test
    fun `access forbidden when no authority`() {

      webTestClient.get().uri("/roles/paged")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {

      webTestClient.get().uri("/roles/paged")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {

      webTestClient.get().uri("/roles/paged")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get all roles defaults`() {
      hmppsAuthMockServer.stubGetAllRolesPaged()
      webTestClient.get().uri("/roles/paged")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].roleName").isEqualTo("Audit viewer")
        .jsonPath("$.content[1].roleName").isEqualTo("Auth Group Manager")
        .jsonPath("$.content[2].roleName").isEqualTo("role 1")
        .jsonPath("$.content[3].roleName").isEqualTo("role 2")
        .jsonPath("$.content[4].roleName").isEqualTo("role 3")
        .jsonPath("$.content[5].roleName").isEqualTo("role 4")
        .jsonPath("$.content[6].roleName").isEqualTo("role 5")
        .jsonPath("$.content[7].roleName").isEqualTo("role 6")
        .jsonPath("$.content[8].roleName").isEqualTo("role 7")
        .jsonPath("$.content[9].roleName").isEqualTo("role 8")
        .jsonPath("$.content.length()").isEqualTo(10)
        .jsonPath("$.size").isEqualTo(10)
        .jsonPath("$.totalElements").isEqualTo(37)
        .jsonPath("$.totalPages").isEqualTo(4)
        .jsonPath("$.last").isEqualTo(false)
    }

    @Test
    fun `get all roles page 3 size 4 descending`() {
      hmppsAuthMockServer.stubGetAllRolesPage3Descending()
      webTestClient.get().uri("/roles/paged?page=3&size=4&sort=roleName,desc")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].roleName").isEqualTo("role 8")
        .jsonPath("$.content[1].roleName").isEqualTo("role 7")
        .jsonPath("$.content[2].roleName").isEqualTo("role 6")
        .jsonPath("$.content[3].roleName").isEqualTo("role 5")
        .jsonPath("$.content.length()").isEqualTo(4)
        .jsonPath("$.size").isEqualTo(4)
        .jsonPath("$.totalElements").isEqualTo(37)
        .jsonPath("$.totalPages").isEqualTo(10)
        .jsonPath("$.last").isEqualTo(false)
    }

    @Test
    fun `get all roles filter role code`() {
      hmppsAuthMockServer.stubGetAllRolesPagedFilterRoleCode()
      webTestClient.get().uri("/roles/paged?roleCode=account")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].roleCode").isEqualTo("ACCOUNT_MANAGER")
    }

    @Test
    fun `get all roles filter role name`() {
      hmppsAuthMockServer.stubGetAllRolesPagedFilterRoleName()
      webTestClient.get().uri("/roles/paged?roleName=manager")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].roleName").isEqualTo("The group account manager")
    }

    @Test
    fun `get all roles filter admin type`() {
      hmppsAuthMockServer.stubGetAllRolesPagedFilterAdminType()
      webTestClient.get().uri("/roles/paged?adminTypes=EXT_ADM")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].adminType[0].adminTypeCode").isEqualTo("EXT_ADM")
    }

    @Test
    fun `get all roles filter multiple admin types`() {
      hmppsAuthMockServer.stubGetAllRolesPagedFilterAdminTypes()
      webTestClient.get().uri("/roles/paged?adminTypes=EXT_ADM&adminTypes=DPS_ADM")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].adminType[0].adminTypeCode").isEqualTo("EXT_ADM")
        .jsonPath("$.content[0].adminType[1].adminTypeCode").isEqualTo("DPS_ADM")
    }

    @Test
    fun `get all roles using all filters`() {
      hmppsAuthMockServer.stubGetAllRolesPagedUsingAllFilters()
      webTestClient.get().uri("/roles/paged?page=1&size=10&sort=roleName,asc&roleCode=account&roleName=manager&adminTypes=EXT_ADM&adminTypes=DPS_ADM")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].adminType[0].adminTypeCode").isEqualTo("EXT_ADM")
        .jsonPath("$.content[0].adminType[1].adminTypeCode").isEqualTo("DPS_ADM")
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
      hmppsAuthMockServer.stubGetRolesDetails("AUTH_GROUP_MANAGER")
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

    @Test
    fun `get role fail - role not found`() {
      hmppsAuthMockServer.stubGetRoleDetailsFail(NOT_FOUND, "AUTH_GROUP_MANAGER")
      webTestClient.get().uri("/roles/AUTH_GROUP_MANAGER")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
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
        .body(fromValue(mapOf("roleName" to "12345".repeat(20) + "y")))
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
    fun `Change role name returns error when role not found`() {
      hmppsAuthMockServer.stubPutRoleNameFail("Not_A_Role", NOT_FOUND)
      webTestClient
        .put().uri("/roles/Not_A_Role")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
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
    fun `Change role name success for DPS Role`() {
      hmppsAuthMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN")
      hmppsAuthMockServer.stubPutRoleName("OAUTH_ADMIN")
      nomisApiMockServer.stubPutRole("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isOk
      nomisApiMockServer.verify(putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN")))
    }

    @Test
    fun `Change role name success for non-DPS Role`() {
      hmppsAuthMockServer.stubGetRolesDetails("OAUTH_ADMIN")
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
      hmppsAuthMockServer.stubGetRolesDetails("OAUTH_ADMIN")
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
      hmppsAuthMockServer.stubPutRoleDescriptionFail("Not_A_Role", NOT_FOUND)
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
      hmppsAuthMockServer.stubGetRolesDetails("Not_A_Role")
      hmppsAuthMockServer.stubPutRoleAdminTypeFail("Not_A_Role", NOT_FOUND)
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
    fun `Change role admin type returns success - creating new DPS Role`() {
      hmppsAuthMockServer.stubGetRolesDetails("OAUTH_ADMIN")
      hmppsAuthMockServer.stubPutRoleAdminType("OAUTH_ADMIN")
      nomisApiMockServer.stubCreateRole()
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM"))))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role admin type returns success - new External Admin Role`() {
      hmppsAuthMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN")
      hmppsAuthMockServer.stubPutRoleAdminType("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM", "EXT_ADM"))))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role admin type returns success - becoming different type of DPS Role`() {
      hmppsAuthMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN")
      hmppsAuthMockServer.stubPutRoleAdminType("OAUTH_ADMIN")
      nomisApiMockServer.stubPutRole("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM", "DPS_LSA"))))
        .exchange()
        .expectStatus().isOk
    }
  }
}

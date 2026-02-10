package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.REQUEST_TIMEOUT
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class RolesControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class CreateRole {

    @AfterEach
    fun resetMocks() {
      externalUsersApiMockServer.resetAll()
      nomisApiMockServer.resetAll()
    }

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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create role`() {
      externalUsersApiMockServer.stubPostCreate("/roles")

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @Test
    fun `create role when role name has 30 characters`() {
      externalUsersApiMockServer.stubPostCreate("/roles")
      nomisApiMockServer.stubCreateRole()

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "12345".repeat(6),
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM", "DPS_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/roles"))
          .withRequestBody(matchingJsonPath("name", equalTo("12345".repeat(6)))),
      )
    }

    @Test
    fun `create role doesn't call external users api if nomis fails`() {
      nomisApiMockServer.stubCreateRoleFail(CONFLICT, "RC1")
      externalUsersApiMockServer.stubPostCreate("/roles")

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("DPS_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Role already exists: Role with code RC1 already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("Role with code RC1 already exists")
        }

      externalUsersApiMockServer.verify(0, postRequestedFor(urlEqualTo("/roles")))
    }

    @Test
    fun `create role when role name has greater than 30 characters`() {
      externalUsersApiMockServer.stubPostCreate("/roles")
      nomisApiMockServer.stubCreateRole()

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "12345".repeat(6) + "y",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM", "DPS_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/roles"))
          .withRequestBody(matchingJsonPath("name", equalTo("12345".repeat(6)))),
      )
    }

    @Test
    fun `create role ROLE_`() {
      externalUsersApiMockServer.stubPostCreate("/roles")

      webTestClient.post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "ROLE_RC2",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated

      externalUsersApiMockServer.verify(
        postRequestedFor(urlEqualTo("/roles"))
          .withRequestBody(
            containing("{\"roleCode\":\"RC2\",\"roleName\":\"new role name\",\"roleDescription\":\"Description\",\"adminType\":[\"EXT_ADM\"]}"),
          ),
      )
    }

    @Test
    fun `create role returns error when role exists`() {
      externalUsersApiMockServer.stubCreateRoleFail(CONFLICT)
      webTestClient
        .post().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf(
              "roleCode" to "RC1",
              "roleName" to "new role name",
              "roleDescription" to "Description",
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("User test message")
          assertThat(it["developerMessage"] as String).isEqualTo("Developer test message")
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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role code must be between 2 and 30 characters")
        }
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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role code must be between 2 and 30 characters")
        }
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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role code must only contain 0-9, A-Z, a-z and _  characters")
        }
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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role name must be between 4 and 100 characters")
        }
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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role name must be between 4 and 100 characters")
        }
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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role name must only contain 0-9, A-Z, a-z and ( ) & , - . '  characters")
        }
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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role description must be no more than 1024 characters")
        }
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
              "adminType" to listOf("EXT_ADM"),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role description must only contain can only contain 0-9, A-Z, a-z, newline and ( ) & , - . '  characters")
        }
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
              "adminType" to listOf<String>(),
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Admin type cannot be empty")
        }
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
              "adminType" to listOf("DOES_NOT_EXIST"),
            ),
          ),
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
    fun `access allowed when correct role`() {
      externalUsersApiMockServer.stubGetRoles()
      webTestClient.get().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
      webTestClient.get().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES")))
        .exchange()
        .expectStatus().isOk
      webTestClient.get().uri("/roles")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `get all roles defaults`() {
      externalUsersApiMockServer.stubGetRoles()
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
      externalUsersApiMockServer.stubGetAllRolesFilterAdminType()
      webTestClient.get().uri("/roles?adminTypes=EXT_ADM")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].adminType[0].adminTypeCode").isEqualTo("EXT_ADM")
    }

    @Test
    fun `access forbidden when no authority to get external admin`() {
      webTestClient.get().uri("/roles?adminTypes=EXT_ADM")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role to fetch external admin`() {
      webTestClient.get().uri("/roles?adminTypes=EXT_ADM")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `get all roles filter multiple admin types`() {
      externalUsersApiMockServer.stubGetAllRolesFilterAdminTypes()
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
      externalUsersApiMockServer.stubGetAllRolesPaged()
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
      externalUsersApiMockServer.stubGetAllRolesPage3Descending()
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
      externalUsersApiMockServer.stubGetAllRolesPagedFilterRoleCode()
      webTestClient.get().uri("/roles/paged?roleCode=account")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].roleCode").isEqualTo("ACCOUNT_MANAGER")
    }

    @Test
    fun `get all roles filter role name`() {
      externalUsersApiMockServer.stubGetAllRolesPagedFilterRoleName()
      webTestClient.get().uri("/roles/paged?roleName=manager")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].roleName").isEqualTo("The group account manager")
    }

    @Test
    fun `get all roles filter admin type`() {
      externalUsersApiMockServer.stubGetAllRolesPagedFilterAdminType()
      webTestClient.get().uri("/roles/paged?adminTypes=EXT_ADM")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].adminType[0].adminTypeCode").isEqualTo("EXT_ADM")
    }

    @Test
    fun `get all roles filter multiple admin types`() {
      externalUsersApiMockServer.stubGetAllRolesPagedFilterAdminTypes()
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
      externalUsersApiMockServer.stubGetAllRolesPagedUsingAllFilters()
      webTestClient.get()
        .uri("/roles/paged?page=1&size=10&sort=roleName,asc&roleCode=account&roleName=manager&adminTypes=EXT_ADM&adminTypes=DPS_ADM")
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
      externalUsersApiMockServer.stubGetRoleDetails("AUTH_GROUP_MANAGER")
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
          """,
        )
    }

    @Test
    fun `get role fail - role not found`() {
      val userMessage = "User message for Get Role details failed"
      val developerMessage = "Developer message for get role details failed"
      externalUsersApiMockServer.stubGet(NOT_FOUND, "/roles/AUTH_GROUP_MANAGER", userMessage, developerMessage)
      webTestClient.get().uri("/roles/AUTH_GROUP_MANAGER")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo(userMessage)
          assertThat(it["developerMessage"] as String).isEqualTo(developerMessage)
        }
    }
  }

  @Nested
  inner class AmendRoleName {

    @AfterEach
    fun resetMocks() {
      externalUsersApiMockServer.resetAll()
      nomisApiMockServer.resetAll()
    }

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
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to FORBIDDEN.value(),
            ),
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
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role name must be between 4 and 100 characters")
        }
    }

    @Test
    fun `Change role name returns error when length too long`() {
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "12345".repeat(20) + "y")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role name must be between 4 and 100 characters")
        }
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
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role name must only contain 0-9, a-z and ( ) & , - . '  characters")
        }
    }

    @Test
    fun `Change role name returns error when role not found`() {
      externalUsersApiMockServer.stubGetRoleDetails("Not_A_Role")

      val userMessage = "User message for PUT Role Name failed"
      val developerMessage = "Developer message for PUT Role Name failed"
      externalUsersApiMockServer.stubPut(NOT_FOUND, "/roles/Not_A_Role", userMessage, developerMessage)
      webTestClient
        .put().uri("/roles/Not_A_Role")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to NOT_FOUND.value(),
              "userMessage" to userMessage,
              "developerMessage" to developerMessage,
            ),
          )
        }
    }

    @Test
    fun `Change role name success for DPS Role`() {
      externalUsersApiMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN")
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN", "", "")
      nomisApiMockServer.stubPutRole("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isOk
      nomisApiMockServer.verify(
        putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN"))
          .withRequestBody(matchingJsonPath("name", equalTo("new role name"))),
      )
      externalUsersApiMockServer.verify(
        putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN"))
          .withRequestBody(matchingJsonPath("roleName", equalTo("new role name"))),
      )
    }

    @Test
    fun `Change role name success for Role with name has 30 characters`() {
      externalUsersApiMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN")
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN", "", "")
      nomisApiMockServer.stubPutRole("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "12345".repeat(6))))
        .exchange()
        .expectStatus().isOk
      nomisApiMockServer.verify(
        putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN"))
          .withRequestBody(matchingJsonPath("name", equalTo("12345".repeat(6)))),
      )
      externalUsersApiMockServer.verify(
        putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN"))
          .withRequestBody(
            matchingJsonPath("roleName", equalTo("12345".repeat(6))),
          ),
      )
    }

    @Test
    fun `Change role name success for Role with name greater than 30 characters`() {
      externalUsersApiMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN")
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN", "", "")
      nomisApiMockServer.stubPutRole("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "12345".repeat(6) + "y")))
        .exchange()
        .expectStatus().isOk
      nomisApiMockServer.verify(
        putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN"))
          .withRequestBody(matchingJsonPath("name", equalTo("12345".repeat(6)))),
      )
      externalUsersApiMockServer.verify(
        putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN"))
          .withRequestBody(matchingJsonPath("roleName", equalTo("12345".repeat(6) + "y"))),
      )
    }

    @Test
    fun `Change role name doesn't call external users if nomis fails`() {
      externalUsersApiMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN")
      nomisApiMockServer.stubPutRoleFail("OAUTH_ADMIN", REQUEST_TIMEOUT)

      webTestClient.put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isEqualTo(REQUEST_TIMEOUT)

      externalUsersApiMockServer.verify(0, putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN")))
    }

    @Test
    fun `Change role name success for non-DPS Role`() {
      externalUsersApiMockServer.stubGetRoleDetails("OAUTH_ADMIN")
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN", "", "")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleName" to "new role name")))
        .exchange()
        .expectStatus().isOk
      externalUsersApiMockServer.verify(
        putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN"))
          .withRequestBody(matchingJsonPath("roleName", equalTo("new role name"))),
      )
      nomisApiMockServer.verify(0, putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN")))
    }

    @Test
    fun `Change role name passes regex validation`() {
      externalUsersApiMockServer.stubGetRoleDetails("OAUTH_ADMIN")
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN", "", "")
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
          assertThat(it).containsAllEntriesOf(
            mapOf("status" to FORBIDDEN.value()),
          )
        }
    }

    @Test
    fun `Change role description returns error when role not found`() {
      val userMessage = "User message for PUT Role Name failed"
      val developerMessage = "Developer message for PUT Role Name failed"
      externalUsersApiMockServer.stubPut(NOT_FOUND, "/roles/Not_A_Role/description", userMessage, developerMessage)
      webTestClient
        .put().uri("/roles/Not_A_Role/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "new role description")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to NOT_FOUND.value(),
              "userMessage" to userMessage,
              "developerMessage" to developerMessage,
            ),
          )
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
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role description must be no more than 1024 characters")
        }
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
        .expectBody()
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Role description must only contain can only contain 0-9, a-z, newline and ( ) & , - . '  characters")
        }
    }

    @Test
    fun `Change role description success`() {
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN/description", "", "")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "new role description")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description returns success for empty roleDescription`() {
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN/description", "", "")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to "")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description returns success for no role description`() {
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN/description", "", "")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/description")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("roleDescription" to null)))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role description passes regex validation`() {
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN/description", "", "")
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
          assertThat(it).containsAllEntriesOf(
            mapOf("status" to FORBIDDEN.value()),
          )
        }
    }

    @Test
    fun `Change role admin type returns error when role not found`() {
      externalUsersApiMockServer.stubGetRoleDetails("Not_A_Role")
      val userMessage = "User message for PUT Role Admin Type failed"
      val developerMessage = "Developer message for PUT Role Admin Type failed"
      externalUsersApiMockServer.stubPut(NOT_FOUND, "/roles/Not_A_Role/admintype", userMessage, developerMessage)
      webTestClient
        .put().uri("/roles/Not_A_Role/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("EXT_ADM"))))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "status" to NOT_FOUND.value(),
              "userMessage" to userMessage,
              "developerMessage" to developerMessage,
            ),
          )
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
        .jsonPath("errors").value<List<String>> { errors ->
          assertThat(errors).contains("Admin type cannot be empty")
        }
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
      externalUsersApiMockServer.stubGetRoleDetails("OAUTH_ADMIN")
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN/admintype", "", "")
      nomisApiMockServer.stubCreateRole()
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM"))))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role admin type does not call external users when nomis fails - creating new DPS Role`() {
      externalUsersApiMockServer.stubGetRoleDetails("OAUTH_ADMIN2")
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN2/admintype", "", "")
      nomisApiMockServer.stubPostFailEmptyResponse("/roles", REQUEST_TIMEOUT)
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN2/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM"))))
        .exchange()
        .expectStatus().isEqualTo(REQUEST_TIMEOUT)

      externalUsersApiMockServer.verify(0, putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN2/admintype")))
    }

    @Test
    fun `Change role admin type returns success - new External Admin Role`() {
      externalUsersApiMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN")
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN/admintype", "", "")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM", "EXT_ADM"))))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role admin type returns success - becoming different type of DPS Role`() {
      externalUsersApiMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN")
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN/admintype", "", "")
      nomisApiMockServer.stubPutRole("OAUTH_ADMIN")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM", "DPS_LSA"))))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Change role admin type does not call external users when nomis fails - becoming different type of DPS Role`() {
      externalUsersApiMockServer.stubGetDPSRoleDetails("OAUTH_ADMIN3")
      nomisApiMockServer.stubPutRoleFail("OAUTH_ADMIN3", REQUEST_TIMEOUT)
      externalUsersApiMockServer.stubPut(OK, "/roles/OAUTH_ADMIN3/admintype", "", "")
      webTestClient
        .put().uri("/roles/OAUTH_ADMIN3/admintype")
        .headers(setAuthorisation(roles = listOf("ROLE_ROLES_ADMIN")))
        .body(fromValue(mapOf("adminType" to listOf("DPS_ADM", "DPS_LSA", "EXT_ADM"))))
        .exchange()
        .expectStatus().isEqualTo(REQUEST_TIMEOUT)

      externalUsersApiMockServer.verify(0, putRequestedFor(urlEqualTo("/roles/OAUTH_ADMIN3/admintype")))
    }
  }
}

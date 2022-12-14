package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class UserSearchControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class FindUsersByEmail {

    @Test
    fun `access forbidden when unauthorised`() {
      webTestClient.get().uri("/externalusers")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should respond with no content when email address null`() {
      webTestClient.get().uri("/externalusers")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `should respond with no content when external users api responds with no content`() {
      val email = "testy@testing.co.uk"
      externalUsersApiMockServer.stubNoUsersFound(email)

      webTestClient.get().uri("/externalusers?email=$email")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNoContent
    }

    @Test
    fun `should respond with user data returned from external users api`() {
      val email = "auth_test2@digital.justice.gov.uk"
      externalUsersApiMockServer.stubUsersByEmail(email)

      webTestClient.get().uri("/externalusers?email=$email")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.[*].userId").value<List<String>> { assertThat(it).hasSize(2) }
        .jsonPath("$[1].userId").isEqualTo("9e84f1e4-59c8-4b10-927a-9cf9e9a30791")
        .jsonPath("$[1].username").isEqualTo("AUTH_EXPIRED")
        .jsonPath("$[1].email").isEqualTo("auth_test2@digital.justice.gov.uk")
        .jsonPath("$[1].firstName").isEqualTo("Auth")
        .jsonPath("$[1].lastName").isEqualTo("Expired")
        .jsonPath("$[1].locked").isEqualTo(false)
        .jsonPath("$[1].enabled").isEqualTo(true)
        .jsonPath("$[1].verified").isEqualTo(true)
        .jsonPath("$[1].lastLoggedIn").isNotEmpty
        .jsonPath("$[1].inactiveReason").isEqualTo("Expired")
    }
  }

  @Nested
  inner class FindUsersByUserName {

    @Test
    fun `access forbidden when unauthorised`() {
      val userName = "fred"
      webTestClient.get().uri("/externalusers/$userName")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should respond with not found when user not found`() {
      val userName = "fred"
      webTestClient.get().uri("/externalusers/$userName")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `should respond with user data returned from external users api`() {
      val userName = "AUTH_ADM"
      externalUsersApiMockServer.stubUsersByUserName(userName)

      webTestClient.get().uri("/externalusers/$userName")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.userId").isEqualTo("5105a589-75b3-4ca0-9433-b96228c1c8f3")
        .jsonPath("$.username").isEqualTo("AUTH_ADM")
        .jsonPath("$.email").isEqualTo("auth_test2@digital.justice.gov.uk")
        .jsonPath("$.firstName").isEqualTo("Auth")
        .jsonPath("$.lastName").isEqualTo("Adm")
        .jsonPath("$.locked").isEqualTo(false)
        .jsonPath("$.enabled").isEqualTo(true)
        .jsonPath("$.verified").isEqualTo(true)
        .jsonPath("$.lastLoggedIn").isNotEmpty
        .jsonPath("$.inactiveReason").isEqualTo("Expired")
    }

    @Test
    fun `should respond with NOT_FOUND from external users api`() {
      val userName = "AUTH_ADM"
      externalUsersApiMockServer.stubNoUsersFoundForUserName(userName)

      webTestClient.get().uri("/externalusers/$userName")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  inner class UserSearch {

    @Test
    fun `access not authorised for unauthorised user`() {
      webTestClient.get().uri("/externalusers/search")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden for user without roles`() {
      webTestClient.get().uri("/externalusers/search")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden for user with incorrect role`() {
      webTestClient.get().uri("/externalusers/search")
        .headers(setAuthorisation(roles = listOf("ADD_SENSITIVE_CASE_NOTES")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should respond with no results when no users found`() {
      externalUsersApiMockServer.stubUserDefaultSearchNoResults()

      webTestClient.get().uri("/externalusers/search")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content").isEmpty
        .jsonPath("$.totalPages").isEqualTo(0)
        .jsonPath("$.totalElements").isEqualTo(0)
        .jsonPath("$.last").isEqualTo(true)
        .jsonPath("$.size").isEqualTo(10)
        .jsonPath("$.number").isEqualTo(0)
        .jsonPath("$.first").isEqualTo(true)
        .jsonPath("$.numberOfElements").isEqualTo(0)
        .jsonPath("$.empty").isEqualTo(true)
        .jsonPath("$.pageable.offset").isEqualTo(0)
        .jsonPath("$.pageable.pageSize").isEqualTo(10)
        .jsonPath("$.pageable.pageNumber").isEqualTo(0)
        .jsonPath("$.pageable.paged").isEqualTo(true)
        .jsonPath("$.pageable.unpaged").isEqualTo(false)
        .jsonPath("$.pageable.sort.empty").isEqualTo(false)
        .jsonPath("$.pageable.sort.sorted").isEqualTo(true)
        .jsonPath("$.pageable.sort.unsorted").isEqualTo(false)
        .jsonPath("$.sort.empty").isEqualTo(false)
        .jsonPath("$.sort.sorted").isEqualTo(true)
        .jsonPath("$.sort.unsorted").isEqualTo(false)
    }

    @Test
    fun `should respond with paged results when users found`() {
      val name = "tester.mctesty@digital.justice.gov.uk"
      val roles = listOf("TESTING", "MORE_TESTING")
      val groups = listOf("TESTING_GROUP", "MORE_TESTING_GROUP")
      val rolesJoined = roles.joinToString(",")
      val groupsJoined = groups.joinToString(",")

      externalUsersApiMockServer.stubUserSearchAllFiltersWithResults(name, roles, groups)

      webTestClient.get().uri("/externalusers/search?name=$name&roles=$rolesJoined&groups=$groupsJoined")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[*].userId").value<List<String>> { assertThat(it).hasSize(2) }
        .jsonPath("$.content[0].userId").isEqualTo("006a9299-ef3d-4990-8604-13cefac706b5")
        .jsonPath("$.content[0].username").isEqualTo("TESTER.MCTESTY@DIGITAL.JUSTICE.GOV.UK")
        .jsonPath("$.content[0].email").isEqualTo("tester.mctesty@digital.justice.gov.uk")
        .jsonPath("$.content[0].firstName").isEqualTo("Tester1")
        .jsonPath("$.content[0].lastName").isEqualTo("McTester1")
        .jsonPath("$.content[0].locked").isEqualTo(false)
        .jsonPath("$.content[0].enabled").isEqualTo(true)
        .jsonPath("$.content[0].verified").isEqualTo(true)
        .jsonPath("$.content[0].lastLoggedIn").isEqualTo("2022-12-14T10:23:04.915132")
        .jsonPath("$.content[0].inactiveReason").isEmpty
        .jsonPath("$.totalPages").isEqualTo(19)
        .jsonPath("$.totalElements").isEqualTo(185)
        .jsonPath("$.last").isEqualTo(false)
        .jsonPath("$.size").isEqualTo(10)
        .jsonPath("$.number").isEqualTo(0)
        .jsonPath("$.first").isEqualTo(true)
        .jsonPath("$.numberOfElements").isEqualTo(2)
        .jsonPath("$.empty").isEqualTo(false)
        .jsonPath("$.pageable.offset").isEqualTo(0)
        .jsonPath("$.pageable.pageSize").isEqualTo(10)
        .jsonPath("$.pageable.pageNumber").isEqualTo(0)
        .jsonPath("$.pageable.paged").isEqualTo(true)
        .jsonPath("$.pageable.unpaged").isEqualTo(false)
        .jsonPath("$.pageable.sort.empty").isEqualTo(false)
        .jsonPath("$.pageable.sort.sorted").isEqualTo(true)
        .jsonPath("$.pageable.sort.unsorted").isEqualTo(false)
        .jsonPath("$.sort.empty").isEqualTo(false)
        .jsonPath("$.sort.sorted").isEqualTo(true)
        .jsonPath("$.sort.unsorted").isEqualTo(false)
    }
  }
}

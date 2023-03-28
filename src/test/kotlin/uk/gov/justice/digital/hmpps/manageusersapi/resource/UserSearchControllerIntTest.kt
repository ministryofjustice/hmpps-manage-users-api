package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class UserSearchControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class UserSearch {

    @Test
    fun `access not authorised for unauthorised user`() {
      webTestClient.get().uri("/users/search")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden for user without correct role`() {
      webTestClient.get().uri("/users/search")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should contain paging`() {
      hmppsAuthMockServer.stubUserDefaultSearchNoResults()
      val sortMap = mapOf(
        "empty" to false,
        "sorted" to true,
        "unsorted" to false,
      )
      val pageableMap = mapOf(
        "offset" to 0,
        "pageSize" to 10,
        "pageNumber" to 0,
        "paged" to true,
        "unpaged" to false,
        "sort" to sortMap,
      )

      webTestClient.get().uri("/users/search")
        .headers(setAuthorisation(roles = listOf("ROLE_PCMS_USER_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "content" to listOf<Any>(),
              "sort" to sortMap,
              "totalPages" to 0,
              "totalElements" to 0,
              "last" to true,
              "size" to 10,
              "number" to 0,
              "first" to true,
              "numberOfElements" to 0,
              "empty" to true,
              "pageable" to pageableMap,
            ),
          )
        }
    }

    @Test
    fun `should respond with no results when no users found`() {
      hmppsAuthMockServer.stubUserDefaultSearchNoResults()

      webTestClient.get().uri("/users/search")
        .headers(setAuthorisation(roles = listOf("ROLE_PCMS_USER_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content").isEmpty
    }

    @Test
    fun `should respond with paged results when users found`() {
      val name = "tester.mctesty@digital.justice.gov.uk"
      val encodedName = "tester.mctesty%40digital.justice.gov.uk"

      hmppsAuthMockServer.stubUserSearchAllFiltersWithResults(encodedName)

      webTestClient.get().uri("/users/search?name=$name")
        .headers(setAuthorisation(roles = listOf("ROLE_PCMS_USER_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[*].userId").value<List<String>> { assertThat(it).hasSize(2) }
        .jsonPath("$.content[0]").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "userId" to "006a9299-ef3d-4990-8604-13cefac706b5",
              "username" to "TESTER.MCTESTY@DIGITAL.JUSTICE.GOV.UK",
              "email" to "tester.mctesty@digital.justice.gov.uk",
              "firstName" to "Tester1",
              "lastName" to "McTester1",
              "locked" to false,
              "enabled" to true,
              "verified" to true,
              "lastLoggedIn" to "2022-12-14T10:23:04.915132",
              "inactiveReason" to null,
            ),
          )
        }
    }

    @Test
    fun `should correctly encode query parameters`() {
      val name = "tester.mctesty+email@digital.justice.gov.uk"
      val encodedName = "tester.mctesty%20email%40digital.justice.gov.uk"

      hmppsAuthMockServer.stubUserSearchEncodedQueryParams(encodedName)

      webTestClient.get().uri("/users/search?name=$name&status=ALL&page=0&size=10")
        .headers(setAuthorisation(roles = listOf("ROLE_PCMS_USER_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].username").isEqualTo("TESTER.MCTESTY+EMAIL@DIGITAL.JUSTICE.GOV.UK")
        .jsonPath("$.content[0].email").isEqualTo("tester.mctesty+email@digital.justice.gov.uk")
    }
  }
}

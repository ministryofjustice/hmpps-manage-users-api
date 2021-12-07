package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleDifferences
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleDifferences.UpdateType.NONE
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleSyncService
import uk.gov.justice.digital.hmpps.manageusersapi.service.SyncStatistics

class SyncControllerIntTest : IntegrationTestBase() {

  @MockBean
  private lateinit var syncService: RoleSyncService

  @TestInstance(Lifecycle.PER_CLASS)
  @Nested
  inner class SecurePutEndpoints {
    private fun secureEndpoints() =
      listOf(
        "/sync/roles",
      )

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires a valid authentication token`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires the correct role`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role but wrong scope`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN"), scopes = listOf("read")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role`(uri: String) {

      val rd = RoleDifferences(
        "ROLE_CODE_1",
        "not equal: only on left={name=A test role}",
        updateType = NONE
      )

      val mapper = mutableMapOf<String, RoleDifferences>()
      mapper["ROLE_CODE_1"] = rd
      whenever(syncService.sync(false)).thenReturn(
        SyncStatistics(mapper)
      )

      webTestClient.put()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN"), scopes = listOf("write", "read")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          """
          {
          "roles":
            {
              "ROLE_CODE_1":
                {
                  "roleCode":"ROLE_CODE_1",
                  "differences":"not equal: only on left={name=A test role}",
                  "updateType":"NONE"
                }
              }
            }
      """
        )
    }
  }

  @TestInstance(Lifecycle.PER_CLASS)
  @Nested
  inner class SecureGetEndpoints {
    private fun secureEndpoints() =
      listOf(
        "/sync/roles",
      )

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires a valid authentication token`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires the correct role`(uri: String) {
      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role`(uri: String) {

      val rd = RoleDifferences(
        "ROLE_CODE_1",
        "not equal: only on left={name=A test role}",
        updateType = NONE
      )

      val mapper = mutableMapOf<String, RoleDifferences>()
      mapper["ROLE_CODE_1"] = rd
      whenever(syncService.sync(true)).thenReturn(
        SyncStatistics(mapper)
      )

      webTestClient.get()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          """
          {
          "roles":
            {
              "ROLE_CODE_1":
                {
                  "roleCode":"ROLE_CODE_1",
                  "differences":"not equal: only on left={name=A test role}",
                  "updateType":"NONE"
                }
              }
            }
      """
        )
    }
  }
}

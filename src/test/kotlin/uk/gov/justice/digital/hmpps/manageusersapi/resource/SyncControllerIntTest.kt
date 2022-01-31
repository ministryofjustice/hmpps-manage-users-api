package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleSyncService
import uk.gov.justice.digital.hmpps.manageusersapi.service.SyncDifferences
import uk.gov.justice.digital.hmpps.manageusersapi.service.SyncDifferences.UpdateType.NONE
import uk.gov.justice.digital.hmpps.manageusersapi.service.SyncStatistics
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserSyncService

class SyncControllerIntTest : IntegrationTestBase() {

  @MockBean
  private lateinit var roleSyncService: RoleSyncService

  @MockBean
  private lateinit var userSyncService: UserSyncService

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

      val rd = SyncDifferences(
        "ROLE_CODE_1",
        "not equal: only on left={name=A test role}",
        updateType = NONE
      )

      val mapper = mutableMapOf<String, SyncDifferences>()
      mapper["ROLE_CODE_1"] = rd
      whenever(roleSyncService.sync(false)).thenReturn(
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
          "results":
            {
              "ROLE_CODE_1":
                {
                  "id":"ROLE_CODE_1",
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
        "/sync/users"
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
  }
  @Test
  internal fun `satisfies the correct role sync roles`() {

    val rd = SyncDifferences(
      "ROLE_CODE_1",
      "not equal: only on left={name=A test role}",
      updateType = NONE
    )

    val mapper = mutableMapOf<String, SyncDifferences>()
    mapper["ROLE_CODE_1"] = rd
    whenever(roleSyncService.sync(true)).thenReturn(
      SyncStatistics(mapper)
    )

    webTestClient.get()
      .uri("/sync/roles")
      .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectBody().json(
        """
          {
          "results":
            {
              "ROLE_CODE_1":
                {
                  "id":"ROLE_CODE_1",
                  "differences":"not equal: only on left={name=A test role}",
                  "updateType":"NONE"
                }
              }
            }
      """
      )
  }

  @Test
  internal fun `satisfies the correct role to sync users`() {

    val diff = SyncDifferences(
      "username1",
      "not equal: only on left={username=A test user}",
      updateType = NONE
    )

    val mapper = mutableMapOf<String, SyncDifferences>()
    mapper["username1"] = diff
    whenever(userSyncService.sync()).thenReturn(
      SyncStatistics(mapper)
    )

    webTestClient.get()
      .uri("/sync/users")
      .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_NOMIS_USER_ACCOUNT", "ROLE_MAINTAIN_OAUTH_USERS")))
      .exchange()
      .expectStatus().isOk
      .expectBody().json(
        """
          {
          "results":
            {
              "username1":
                {
                  "id":"username1",
                  "differences":"not equal: only on left={username=A test user}",
                  "updateType":"NONE"
                }
              }
            }
      """
      )
  }
}

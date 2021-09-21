package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth

class RolesControllerIntTest : IntegrationTestBase() {

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

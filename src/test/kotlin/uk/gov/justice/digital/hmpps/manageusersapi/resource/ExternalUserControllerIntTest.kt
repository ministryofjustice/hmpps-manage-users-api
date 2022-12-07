package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class ExternalUserControllerIntTest: IntegrationTestBase() {
    @Nested
    inner class MyAssignableGroups {

        @Test
        fun `Not accessible without valid token`() {
            webTestClient.get().uri("/externalusers/me/assignable-groups")
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        fun `Responds with groups for authorised user without roles`() {
            externalUsersApiMockServer.stubMyAssignableGroups()

            webTestClient.get().uri("/externalusers/me/assignable-groups")
                .headers(setAuthorisation())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.[*].groupCode").value<List<String>> { assertThat(it).hasSize(2) }
                .jsonPath("$.[0].groupCode").isEqualTo("SITE_1_GROUP_1")
                .jsonPath("$.[0].groupName").isEqualTo("Site 1 - Group 1")
        }
    }
}

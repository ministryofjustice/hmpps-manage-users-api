package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class ValidationControllerIntTest : IntegrationTestBase() {

  @Test
  fun `Should fail for invalid email domain`() {
    externalUsersApiMockServer.stubInvalidEmailDomain()
    val isValid = webTestClient
      .get().uri("/validate/email-domain?emailDomain=invaliddomain.com")
      .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_CREATE_EMAIL_TOKEN")))
      .exchange()
      .expectStatus().isOk
      .expectBody<Boolean>()
      .returnResult().responseBody

    if (isValid != null) {
      assertThat(isValid).isFalse
    }
  }

  @Test
  fun `Validate email domain matching domain`() {
    externalUsersApiMockServer.stubValidEmailDomain()

    val isValid = webTestClient
      .get().uri("/validate/email-domain?emailDomain=gov.uk")
      .headers(setAuthorisation("AUTH_ADM", listOf("ROLE_CREATE_EMAIL_TOKEN")))
      .exchange()
      .expectStatus().isOk
      .expectBody<Boolean>()
      .returnResult().responseBody

    if (isValid != null) {
      assertThat(isValid).isTrue
    }
  }
}

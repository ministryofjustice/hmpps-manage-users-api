package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase

class EmailDomainControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class DomainList {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/email-domains")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/email-domains")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri("/email-domains")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `retrieve domain list`() {
      externalUsersApiMockServer.stubGetEmailDomains()
      webTestClient.get().uri("/email-domains")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$[0].id").isEqualTo("45E266FF-8776-48DB-A2F7-4FA927EFE4C8")
        .jsonPath("$[0].domain").isEqualTo("advancecharity.org.uk")
        .jsonPath("$[0].description").isEqualTo("ADVANCE")
        .jsonPath("$[1].id").isEqualTo("8BB676EE-7531-44BA-9A31-5355BEEAD9DB")
        .jsonPath("$[1].domain").isEqualTo("bidvestnoonan.com")
        .jsonPath("$[1].description").isEqualTo("BIDVESTNOONA")
        .jsonPath("$[2].id").isEqualTo("FFDB69CB-1E23-40F2-B94A-11E6A9AE7BBF")
        .jsonPath("$[2].domain").isEqualTo("bsigroup.com")
        .jsonPath("$[2].description").isEqualTo("BSIGROUP")
        .jsonPath("$[3].id").isEqualTo("2200A597-F47A-439C-9B84-79ADADD72D7C")
        .jsonPath("$[3].domain").isEqualTo("careuk.com")
        .jsonPath("$[3].description").isEqualTo("CAREUK")
    }
  }

  @Nested
  inner class Domain {

    private val id = "45e266ff-8776-48db-a2f7-4fa927efe4c8"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/email-domains/$id")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/email-domains/$id")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri("/email-domains/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `domain not found`() {
    }

    @Test
    fun `retrieve domain`() {
      externalUsersApiMockServer.stubGetEmailDomain(id)
      webTestClient.get().uri("/email-domains/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.id").isEqualTo("45E266FF-8776-48DB-A2F7-4FA927EFE4C8")
        .jsonPath("$.domain").isEqualTo("advancecharity.org.uk")
        .jsonPath("$.description").isEqualTo("ADVANCE")
    }
  }

  @Nested
  inner class CreateDomain {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/email-domains")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/email-domains")
        .headers(setAuthorisation(roles = listOf()))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "name" to "advancecharity.org.uk",
              "description" to "ADVANCE"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/email-domains")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "name" to "advancecharity.org.uk",
              "description" to "ADVANCE"
            )
          )
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `domain already exists`() {
      externalUsersApiMockServer.stubCreateEmailDomainConflict()
      webTestClient.post().uri("/email-domains")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "name" to "advancecharity.org.uk",
              "description" to "ADVANCE"
            )
          )
        )
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectBody()
        .jsonPath("$.status").isEqualTo(HttpStatus.CONFLICT.value())
        .jsonPath("$.userMessage").isEqualTo("Unable to add email domain: Unable to add email domain: advancecharity.org.uk to allowed list with reason: domain already present in allowed list")
        .jsonPath("$.developerMessage").isEqualTo("Unable to add email domain: advancecharity.org.uk to allowed list with reason: domain already present in allowed list")
    }

    @Test
    fun create() {
      externalUsersApiMockServer.stubCreateEmailDomain()
      webTestClient.post().uri("/email-domains")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
        .body(
          BodyInserters.fromValue(
            mapOf(
              "name" to "advancecharity.org.uk",
              "description" to "ADVANCE"
            )
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.id").isEqualTo("45E266FF-8776-48DB-A2F7-4FA927EFE4C8")
        .jsonPath("$.domain").isEqualTo("advancecharity.org.uk")
        .jsonPath("$.description").isEqualTo("ADVANCE")
    }
  }

  @Nested
  inner class DeleteDomain {

    private val id = "45e266ff-8776-48db-a2f7-4fa927efe4c8"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/email-domains/$id")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/email-domains/$id")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.delete().uri("/email-domains/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun delete() {
      webTestClient.delete().uri("/email-domains/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_EMAIL_DOMAINS")))
        .exchange()
        .expectStatus().isOk
    }
  }
}

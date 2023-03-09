package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import java.util.UUID

class UserControllerIntTest : IntegrationTestBase() {
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

  @Nested
  inner class DisableExternalUser {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/disable")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/disable")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(mapOf("reason" to "bob")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/disable")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .body(BodyInserters.fromValue(mapOf("reason" to "bob")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `error when no body`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/disable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun disableUser() {
      externalUsersApiMockServer.stubPutDisableUser("2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f")
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f/disable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .body(BodyInserters.fromValue(mapOf("reason" to "bob")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  inner class EnableExternalUser {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun ` should fail with not_found for invalid user id`() {
      externalUsersApiMockServer.stubPutEnableInvalidUser("2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f")
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNotFound
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(HttpStatus.NOT_FOUND.value())
          assertThat(it["userMessage"] as String)
            .startsWith("User not found: User 2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f not found")
          assertThat(it["developerMessage"] as String)
            .startsWith("User 2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f not found")
        }
    }

    @Test
    fun `should fail with forbidden  for user not in group`() {
      externalUsersApiMockServer.stubPutEnableFailUserNotInGroup()
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isForbidden
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["status"] as Int).isEqualTo(HttpStatus.FORBIDDEN.value())
          assertThat(it["userMessage"] as String)
            .startsWith("User group relationship exception: Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups")
          assertThat(it["developerMessage"] as String)
            .startsWith("Unable to maintain user: AUTH_BULK_AMEND_EMAIL with reason: User not with your groups")
        }
    }

    @Test
    fun enableUser() {
      externalUsersApiMockServer.stubPutEnableUser("2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f")
      webTestClient.put().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d2f/enable")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_OAUTH_USERS", "ROLE_AUTH_GROUP_MANAGER")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  inner class AlterUserEmail {

    @Test
    fun `Not accessible without valid token`() {
      webTestClient.post().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/email")
        .body(BodyInserters.fromValue(mapOf("email" to "bobby.b@digital.justice.gov.uk")))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `Not accessible without correct role`() {
      webTestClient.post().uri("/externalusers/2e285ccd-dcfd-4497-9e28-d6e8e10a2d3f/email")
        .headers(setAuthorisation("ITAG_USER_ADM"))
        .body(BodyInserters.fromValue(mapOf("email" to "bobby.b@digital.justice.gov.uk")))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()
        .json(
          """
            {"status":403,"userMessage":"Access is denied","developerMessage":"Access is denied"}
          """.trimIndent()
        )
    }

    @Test
    fun `Fails to alter user email for user whose username is email address and email already taken`() {
      externalUsersApiMockServer.stubUserById("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", "bob@testing.co.uk")
      externalUsersApiMockServer.stubUserHasPassword("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", true)
      hmppsAuthMockServer.stubForTokenByEmailType()
      externalUsersApiMockServer.stubValidateEmailDomain("justice.gov.uk", true)
      externalUsersApiMockServer.stubUserByUsername("auth_user_email_test@justice.gov.uk".uppercase())

      webTestClient
        .post().uri("/externalusers/2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F/email")
        .body(BodyInserters.fromValue(mapOf("email" to "auth_user_email_test@justice.gov.uk")))
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .json(
          """
            {"status":400,"developerMessage":"Validate email failed with reason: duplicate"}
          """.trimIndent()
        )
    }

    @Test
    fun `Succeeds to alter user email`() {
      externalUsersApiMockServer.stubUserById("67A789DE-7D29-4863-B9C2-F2CE715DC4BC")
      externalUsersApiMockServer.stubUserHasPassword("67A789DE-7D29-4863-B9C2-F2CE715DC4BC", true)
      hmppsAuthMockServer.stubForTokenByEmailType()
      externalUsersApiMockServer.stubValidateEmailDomain("digital.justice.gov.uk", true)
      externalUsersApiMockServer.stubPutEmailAndUsername(
        "67A789DE-7D29-4863-B9C2-F2CE715DC4BC", "bobby.b@digital.justice.gov.uk", "EXT_TEST"
      )

      webTestClient
        .post().uri("/externalusers/67A789DE-7D29-4863-B9C2-F2CE715DC4BC/email")
        .body(BodyInserters.fromValue(mapOf("email" to "bobby.b@digital.justice.gov.uk")))
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Amends username and email address`() {
      val userName = "bobby.b@digital.justice.gov.uk".uppercase()
      externalUsersApiMockServer.stubUserById("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", "bob@testing.co.uk")
      externalUsersApiMockServer.stubUserHasPassword("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", true)
      hmppsAuthMockServer.stubForTokenByEmailType()
      externalUsersApiMockServer.stubValidateEmailDomain("digital.justice.gov.uk", true)
      externalUsersApiMockServer.stubNoUsersFound("/users/$userName", userName)
      externalUsersApiMockServer.stubPutEmailAndUsername(
        "2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", "bobby.b@digital.justice.gov.uk", "bobby.b@digital.justice.gov.uk"
      )

      webTestClient
        .post().uri("/externalusers/2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F/email")
        .body(BodyInserters.fromValue(mapOf("email" to "bobby.b@digital.justice.gov.uk")))
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Amends username and email address for user without password`() {
      val userName = "bobby.b@digital.justice.gov.uk".uppercase()
      externalUsersApiMockServer.stubUserById("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", "EXT_TEST@DIGITAL.JUSTICE.GOV.UK")
      externalUsersApiMockServer.stubUserHasPassword("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", false)
      externalUsersApiMockServer.stubValidateEmailDomain("digital.justice.gov.uk", true)

      hmppsAuthMockServer.stubResetTokenForUser("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F")
      hmppsAuthMockServer.stubServiceDetailsByServiceCode("prison-staff-hub")
      externalUsersApiMockServer.stubGetUserGroups(UUID.fromString("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F"), false)
      externalUsersApiMockServer.stubNoUsersFound("/users/$userName", userName)
      externalUsersApiMockServer.stubPutEmailAndUsername(
        "2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", "bobby.b@digital.justice.gov.uk", "bobby.b@digital.justice.gov.uk"
      )

      webTestClient
        .post().uri("/externalusers/2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F/email")
        .body(BodyInserters.fromValue(mapOf("email" to "bobby.b@digital.justice.gov.uk")))
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `Amends email address for user without password`() {
      externalUsersApiMockServer.stubUserById("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F")
      externalUsersApiMockServer.stubUserHasPassword("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", false)
      externalUsersApiMockServer.stubValidateEmailDomain("digital.justice.gov.uk", true)

      hmppsAuthMockServer.stubResetTokenForUser("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F")
      hmppsAuthMockServer.stubServiceDetailsByServiceCode("prison-staff-hub")
      externalUsersApiMockServer.stubGetUserGroups(UUID.fromString("2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F"), false)
      externalUsersApiMockServer.stubPutEmailAndUsername(
        "2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F", "bobby.b@digital.justice.gov.uk", "EXT_TEST"
      )

      webTestClient
        .post().uri("/externalusers/2E285CCD-DCFD-4497-9E24-D6E8E10A2D3F/email")
        .body(BodyInserters.fromValue(mapOf("email" to "bobby.b@digital.justice.gov.uk")))
        .headers(setAuthorisation("ITAG_USER_ADM", listOf("ROLE_MAINTAIN_OAUTH_USERS")))
        .exchange()
        .expectStatus().isOk
    }
  }
}

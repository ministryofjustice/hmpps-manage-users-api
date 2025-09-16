package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUsageType
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserDetails

class UserControllerIntTest : IntegrationTestBase() {

  @Nested
  inner class AmendUserEmail {
    private val username = "testy"
    private val domain = "testing.com"
    private val newEmailAddress = "new.testy@$domain"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/prisonusers/$username/email")
        .body(
          fromValue(
            mapOf("email" to newEmailAddress),
          ),
        )
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/prisonusers/$username/email")
        .headers(setAuthorisation(roles = listOf()))
        .body(
          fromValue(
            mapOf("email" to newEmailAddress),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/prisonusers/$username/email")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .body(
          fromValue(
            mapOf("email" to newEmailAddress),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `amend user email`() {
      nomisApiMockServer.stubFindUserByUsername(username)
      hmppsAuthMockServer.stubConfirmRecognised(username)
      externalUsersApiMockServer.stubValidateEmailDomain(domain, true)
      hmppsAuthMockServer.stubForTokenByEmailType()
      hmppsAuthMockServer.stubUpdatePrisonUserEmail(username, newEmailAddress)

      webTestClient.post().uri("/prisonusers/$username/email")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf("email" to newEmailAddress),
          ),
        )
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `remote failure passed on`() {
      nomisApiMockServer.stubFindUserByUsername(username)
      hmppsAuthMockServer.stubConfirmRecognised(username)
      externalUsersApiMockServer.stubValidateEmailDomain(domain, true)
      hmppsAuthMockServer.stubForTokenByEmailType()
      hmppsAuthMockServer.stubPutFail("/auth/api/prisonuser/$username/email", HttpStatus.INTERNAL_SERVER_ERROR)

      webTestClient.post().uri("/prisonusers/$username/email")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .body(
          fromValue(
            mapOf("email" to newEmailAddress),
          ),
        )
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()
        .jsonPath("status").isEqualTo("500")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Auth User message for PUT failed")
          assertThat(it["developerMessage"] as String).isEqualTo("Developer Auth user message for PUT failed")
        }
    }
  }

  @Nested
  inner class CreateUser {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/prisonusers")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf()))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong scope`() {
      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_ROLE"), scopes = listOf("read")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create Central Admin user`() {
      nomisApiMockServer.stubCreateCentralAdminUser()
      hmppsAuthMockServer.stubForNewToken()
      externalUsersApiMockServer.stubValidEmailDomain()

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST_ADM",
              "email" to "testadm@gov.uk",
              "firstName" to "Testadm",
              "lastName" to "User",
              "userType" to "DPS_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to "TEST_ADM",
              "firstName" to "Testadm",
              "lastName" to "User",
              "primaryEmail" to "testadm@test.com",
            ),
          )
        }

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/admin-account"))
          .withRequestBody(
            containing(
              """
              {"username":"TEST_ADM","email":"testadm@gov.uk","firstName":"Testadm","lastName":"User"}
              """.trimIndent(),
            ),
          ),
      )
    }

    @Test
    fun `create General user`() {
      nomisApiMockServer.stubCreateGeneralUser()
      hmppsAuthMockServer.stubForNewToken()

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST_GEN",
              "email" to "testgen@gov.uk",
              "firstName" to "Testgen",
              "lastName" to "User",
              "userType" to "DPS_GEN",
              "defaultCaseloadId" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to "TEST_GEN",
              "firstName" to "Testgen",
              "lastName" to "User",
              "primaryEmail" to "testgen@test.com",
            ),
          )
        }

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/general-account"))
          .withRequestBody(
            containing(
              """
              {"username":"TEST_GEN","email":"testgen@gov.uk","firstName":"Testgen","lastName":"User","defaultCaseloadId":"MDI"}
              """.trimIndent(),
            ),
          ),
      )
    }

    @Test
    fun `create Local admin user`() {
      nomisApiMockServer.stubCreateLocalAdminUser()
      hmppsAuthMockServer.stubForNewToken()

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST_LADM",
              "email" to "testladm@gov.uk",
              "firstName" to "Testladm",
              "lastName" to "User",
              "userType" to "DPS_LSA",
              "defaultCaseloadId" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it).containsExactlyInAnyOrderEntriesOf(
            mapOf(
              "username" to "TEST_LADM",
              "firstName" to "Testladm",
              "lastName" to "User",
              "primaryEmail" to "testladm@test.com",
            ),
          )
        }

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/local-admin-account"))
          .withRequestBody(
            containing(
              """
              {"username":"TEST_LADM","email":"testladm@gov.uk","firstName":"Testladm","lastName":"User","localAdminGroup":"MDI"}
              """.trimIndent(),
            ),
          ),
      )
    }

    @Test
    fun `create central admin user returns error when username already exists`() {
      nomisApiMockServer.stubCreateCentralAdminUserConflict()
      hmppsAuthMockServer.stubForNewToken()
      externalUsersApiMockServer.stubValidEmailDomain()

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["errorCode"] as Int).isEqualTo(601)
          assertThat(it["userMessage"] as String).isEqualTo("User already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("Unable to create user: username TEST1 already exists")
        }
    }

    @Test
    fun `create central admin user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateCentralAdminUserWithErrorFail(BAD_REQUEST)

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "DUD_FIRST_NAME",
              "lastName" to "User",
              "userType" to "DPS_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(BAD_REQUEST)
        .expectBody()
        .jsonPath("status").isEqualTo("400")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Validation failure: First name must consist of alphabetical characters only and a max 35 chars")
          assertThat(it["developerMessage"] as String).isEqualTo("A bigger message")
        }
    }

    @Test
    fun `create general user returns error when username already exists`() {
      nomisApiMockServer.stubCreateGeneralUserConflict()
      externalUsersApiMockServer.stubValidEmailDomain()

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_GEN",
              "defaultCaseloadId" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["errorCode"] as Int).isEqualTo(601)
          assertThat(it["userMessage"] as String).isEqualTo("User already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("Unable to create user: username TEST1 already exists")
        }
    }

    @Test
    fun `create general user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateGeneralUserWithErrorFail(BAD_REQUEST)

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "DUD_FIRST_NAME",
              "lastName" to "User",
              "userType" to "DPS_GEN",
              "defaultCaseloadId" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(BAD_REQUEST)
        .expectBody()
        .jsonPath("status").isEqualTo("400")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Validation failure: First name must consist of alphabetical characters only and a max 35 chars")
          assertThat(it["developerMessage"] as String).isEqualTo("A bigger message")
        }
    }

    @Test
    fun `create local admin user returns error when username already exists`() {
      nomisApiMockServer.stubCreateLocalAdminUserConflict()
      externalUsersApiMockServer.stubValidEmailDomain()

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_LSA",
              "defaultCaseloadId" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["errorCode"] as Int).isEqualTo(601)
          assertThat(it["userMessage"] as String).isEqualTo("User already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("Unable to create user: username TEST1 already exists")
        }
    }

    @Test
    fun `create local admin user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateLocalAdminUserWithErrorFail(BAD_REQUEST)

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "DUD_FIRST_NAME",
              "lastName" to "User",
              "userType" to "DPS_LSA",
              "defaultCaseloadId" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(BAD_REQUEST)
        .expectBody()
        .jsonPath("status").isEqualTo("400")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Validation failure: First name must consist of alphabetical characters only and a max 35 chars")
          assertThat(it["developerMessage"] as String).isEqualTo("A bigger message")
        }
    }

    @Test
    fun `create local admin user returns error for invalid email domain `() {
      nomisApiMockServer.stubCreateLocalAdminUserConflict()
      externalUsersApiMockServer.stubInvalidEmailDomain()

      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@invaliddomain.com",
              "firstName" to "Test",
              "lastName" to "User",
              "userType" to "DPS_LSA",
              "defaultCaseloadId" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["errorCode"] as Int).isEqualTo(602)
          assertThat(it["userMessage"] as String).isEqualTo("Invalid Email domain: invaliddomain.com with reason: Email domain not valid")
          assertThat(it["developerMessage"] as String).isEqualTo("Invalid Email domain: invaliddomain.com with reason: Email domain not valid")
        }
    }

    @Test
    fun `create user returns error when user type does not exist`() {
      webTestClient.post().uri("/prisonusers")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "username" to "TEST1",
              "email" to "test@gov.uk",
              "firstName" to "Test",
              "lastName" to "User",
            ),
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  inner class CreateLinkedCentralAdminUser {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/linkedprisonusers/admin")
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TESTUSER1",
              "adminUsername" to "TESTUSER1_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/linkedprisonusers/admin")
        .headers(setAuthorisation(roles = listOf()))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TESTUSER1",
              "adminUsername" to "TESTUSER1_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/linkedprisonusers/admin")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TESTUSER1",
              "adminUsername" to "TESTUSER1_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong scope`() {
      webTestClient.post().uri("/linkedprisonusers/admin")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_ROLE"), scopes = listOf("read")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TESTUSER1",
              "adminUsername" to "TESTUSER1_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Link a Central Admin user to a General User`() {
      val createLinkedCentralAdminUserRequest = CreateLinkedCentralAdminUserRequest("TEST_USER", "TEST_USER_ADM")

      nomisApiMockServer.stubCreateLinkedCentralAdminUser(createLinkedCentralAdminUserRequest)
      hmppsAuthMockServer.stubForNewToken()

      val prisonStaffUser = webTestClient.post().uri("/linkedprisonusers/admin")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TEST_USER",
              "adminUsername" to "TEST_USER_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(PrisonStaffUserDto::class.java)
        .returnResult().responseBody!!

      assertThat(prisonStaffUser.staffId).isEqualTo(100L)
      assertThat(prisonStaffUser.firstName).isEqualTo("First")
      assertThat(prisonStaffUser.lastName).isEqualTo("Last")
      assertThat(prisonStaffUser.status).isEqualTo("ACTIVE")
      assertThat(prisonStaffUser.primaryEmail).isEqualTo("f.l@justice.gov.uk")
      assertThat(prisonStaffUser.generalAccount?.username).isEqualTo("TESTUSER1")
      assertThat(prisonStaffUser.generalAccount?.active).isEqualTo(false)
      assertThat(prisonStaffUser.generalAccount?.accountType).isEqualTo(PrisonUsageType.GENERAL)
      assertThat(prisonStaffUser.generalAccount?.activeCaseload?.id).isEqualTo("BXI")
      assertThat(prisonStaffUser.generalAccount?.activeCaseload?.name).isEqualTo("Brixton (HMP)")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(0)?.id ?: String).isEqualTo("NWEB")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(0)?.name ?: String).isEqualTo("Nomis-web Application")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(1)?.id ?: String).isEqualTo("BXI")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(1)?.name ?: String).isEqualTo("Brixton (HMP)")
      assertThat(prisonStaffUser.adminAccount?.username).isEqualTo("TESTUSER1_ADM")
      assertThat(prisonStaffUser.adminAccount?.active).isEqualTo(false)
      assertThat(prisonStaffUser.adminAccount?.accountType).isEqualTo(PrisonUsageType.ADMIN)
      assertThat(prisonStaffUser.adminAccount?.activeCaseload?.id).isEqualTo("CADM_I")
      assertThat(prisonStaffUser.adminAccount?.activeCaseload?.name).isEqualTo("Central Administration Caseload For Hmps")
      assertThat(prisonStaffUser.adminAccount?.caseloads?.get(0)?.id ?: String).isEqualTo("NWEB")
      assertThat(prisonStaffUser.adminAccount?.caseloads?.get(0)?.name ?: String).isEqualTo("Nomis-web Application")
      assertThat(prisonStaffUser.adminAccount?.caseloads?.get(1)?.id ?: String).isEqualTo("CADM_I")
      assertThat(
        prisonStaffUser.adminAccount?.caseloads?.get(1)?.name ?: String,
      ).isEqualTo("Central Administration Caseload For Hmps")

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/link-admin-account/${createLinkedCentralAdminUserRequest.existingUsername}"))
          .withRequestBody(
            containing(
              """
              {"username":"${createLinkedCentralAdminUserRequest.adminUsername}"}
              """.trimIndent(),
            ),
          ),
      )
    }

    @Test
    fun `create linked central admin user returns error when the admin user already exists`() {
      nomisApiMockServer.stubConflictOnPostTo("/users/link-admin-account/TEST_USER")
      webTestClient.post().uri("/linkedprisonusers/admin")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TEST_USER",
              "adminUsername" to "TEST_USER_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo(CONFLICT.value())
        .jsonPath("$.userMessage").isEqualTo("User test message")
        .jsonPath("$.developerMessage").isEqualTo("Developer test message")
    }

    @Test
    fun `create linked central admin user returns error when specified general user is not found`() {
      nomisApiMockServer.stubNotFoundOnPostTo("/users/link-admin-account/TEST_USER_NOT_FOUND")
      webTestClient.post().uri("/linkedprisonusers/admin")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TEST_USER_NOT_FOUND",
              "adminUsername" to "TEST_USER_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(NOT_FOUND)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo(NOT_FOUND.value())
        .jsonPath("$.userMessage").isEqualTo("User test message")
        .jsonPath("$.developerMessage").isEqualTo("Developer test message")
    }

    @Test
    fun `create linked central admin user call passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubSpecifiedHttpStatusOnPostTo("/users/link-admin-account/TEST_USER", BAD_REQUEST)
      webTestClient.post().uri("/linkedprisonusers/admin")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TEST_USER",
              "adminUsername" to "TEST_USER_ADM",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(BAD_REQUEST)
        .expectBody()
        .jsonPath("status").isEqualTo(BAD_REQUEST.value())
        .jsonPath("$.userMessage").isEqualTo("User test message")
        .jsonPath("$.developerMessage").isEqualTo("Developer test message")
    }
  }

  @Nested
  inner class CreateLinkedLocalAdminUser {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/linkedprisonusers/lsa")
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TESTUSER1",
              "adminUsername" to "TESTUSER1_ADM",
              "localAdminGroup" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/linkedprisonusers/lsa")
        .headers(setAuthorisation(roles = listOf()))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TESTUSER1",
              "adminUsername" to "TESTUSER1_ADM",
              "localAdminGroup" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/linkedprisonusers/lsa")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TESTUSER1",
              "adminUsername" to "TESTUSER1_ADM",
              "localAdminGroup" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong scope`() {
      webTestClient.post().uri("/linkedprisonusers/lsa")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_ROLE"), scopes = listOf("read")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TESTUSER1",
              "adminUsername" to "TESTUSER1_ADM",
              "localAdminGroup" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Link a Local Admin user to a General User`() {
      val createLinkedLsaRequest = CreateLinkedLocalAdminUserRequest("TEST_USER", "TEST_USER_ADM", "MDI")

      nomisApiMockServer.stubCreateLinkedLocalAdminUser(createLinkedLsaRequest)

      val prisonStaffUser = webTestClient.post().uri("/linkedprisonusers/lsa")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TEST_USER",
              "adminUsername" to "TEST_USER_ADM",
              "localAdminGroup" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(PrisonStaffUserDto::class.java)
        .returnResult().responseBody!!

      assertThat(prisonStaffUser.staffId).isEqualTo(100L)
      assertThat(prisonStaffUser.firstName).isEqualTo("First")
      assertThat(prisonStaffUser.lastName).isEqualTo("Last")
      assertThat(prisonStaffUser.status).isEqualTo("ACTIVE")
      assertThat(prisonStaffUser.primaryEmail).isEqualTo("f.l@justice.gov.uk")
      assertThat(prisonStaffUser.generalAccount?.username).isEqualTo("TESTUSER1")
      assertThat(prisonStaffUser.generalAccount?.active).isEqualTo(false)
      assertThat(prisonStaffUser.generalAccount?.accountType).isEqualTo(PrisonUsageType.GENERAL)
      assertThat(prisonStaffUser.generalAccount?.activeCaseload?.id).isEqualTo("BXI")
      assertThat(prisonStaffUser.generalAccount?.activeCaseload?.name).isEqualTo("Brixton (HMP)")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(0)?.id ?: String).isEqualTo("NWEB")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(0)?.name ?: String).isEqualTo("Nomis-web Application")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(1)?.id ?: String).isEqualTo("BXI")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(1)?.name ?: String).isEqualTo("Brixton (HMP)")
      assertThat(prisonStaffUser.adminAccount?.username).isEqualTo("TESTUSER1_ADM")
      assertThat(prisonStaffUser.adminAccount?.active).isEqualTo(false)
      assertThat(prisonStaffUser.adminAccount?.accountType).isEqualTo(PrisonUsageType.ADMIN)
      assertThat(prisonStaffUser.adminAccount?.activeCaseload?.id).isEqualTo("CADM_I")
      assertThat(prisonStaffUser.adminAccount?.activeCaseload?.name).isEqualTo("Central Administration Caseload For Hmps")
      assertThat(prisonStaffUser.adminAccount?.caseloads?.get(0)?.id ?: String).isEqualTo("NWEB")
      assertThat(prisonStaffUser.adminAccount?.caseloads?.get(0)?.name ?: String).isEqualTo("Nomis-web Application")
      assertThat(prisonStaffUser.adminAccount?.caseloads?.get(1)?.id ?: String).isEqualTo("CADM_I")
      assertThat(
        prisonStaffUser.adminAccount?.caseloads?.get(1)?.name ?: String,
      ).isEqualTo("Central Administration Caseload For Hmps")

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/link-local-admin-account/${createLinkedLsaRequest.existingUsername}"))
          .withRequestBody(
            containing(
              """
              {"username":"${createLinkedLsaRequest.adminUsername}","localAdminGroup":"${createLinkedLsaRequest.localAdminGroup}"}
              """.trimIndent(),
            ),
          ),
      )
    }

    @Test
    fun `create linked local admin user returns error when the admin user already exists`() {
      nomisApiMockServer.stubConflictOnPostTo("/users/link-local-admin-account/TEST_USER")
      webTestClient.post().uri("/linkedprisonusers/lsa")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TEST_USER",
              "adminUsername" to "TEST_USER_ADM",
              "localAdminGroup" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo(CONFLICT.value())
        .jsonPath("$.userMessage").isEqualTo("User test message")
        .jsonPath("$.developerMessage").isEqualTo("Developer test message")
    }

    @Test
    fun `create linked local admin user returns error when specified general user is not found`() {
      nomisApiMockServer.stubNotFoundOnPostTo("/users/link-local-admin-account/TEST_USER_NOT_FOUND")
      webTestClient.post().uri("/linkedprisonusers/lsa")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TEST_USER_NOT_FOUND",
              "adminUsername" to "TEST_USER_ADM",
              "localAdminGroup" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(NOT_FOUND)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo(NOT_FOUND.value())
        .jsonPath("$.userMessage").isEqualTo("User test message")
        .jsonPath("$.developerMessage").isEqualTo("Developer test message")
    }

    @Test
    fun `create linked local admin user call passes through error when bad request error thrown from nomisapi`() {
      nomisApiMockServer.stubSpecifiedHttpStatusOnPostTo("/users/link-local-admin-account/TEST_USER", BAD_REQUEST)
      webTestClient.post().uri("/linkedprisonusers/lsa")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingUsername" to "TEST_USER",
              "adminUsername" to "TEST_USER_ADM",
              "localAdminGroup" to "MDI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(BAD_REQUEST)
        .expectBody()
        .jsonPath("status").isEqualTo(BAD_REQUEST.value())
        .jsonPath("$.userMessage").isEqualTo("User test message")
        .jsonPath("$.developerMessage").isEqualTo("Developer test message")
    }
  }

  @Nested
  inner class CreateLinkedGeneralUser {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/linkedprisonusers/general")
        .body(
          fromValue(
            mapOf(
              "existingAdminUsername" to "TESTUSER1_ADM",
              "generalUsername" to "TESTUSER1_GEN",
              "defaultCaseloadId" to "BXI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/linkedprisonusers/general")
        .headers(setAuthorisation(roles = listOf()))
        .body(
          fromValue(
            mapOf(
              "existingAdminUsername" to "TESTUSER1_ADM",
              "generalUsername" to "TESTUSER1_GEN",
              "defaultCaseloadId" to "BXI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/linkedprisonusers/general")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .body(
          fromValue(
            mapOf(
              "existingAdminUsername" to "TESTUSER1_ADM",
              "generalUsername" to "TESTUSER1_GEN",
              "defaultCaseloadId" to "BXI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong scope`() {
      webTestClient.post().uri("/linkedprisonusers/general")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_ROLE"), scopes = listOf("read")))
        .body(
          fromValue(
            mapOf(
              "existingAdminUsername" to "TESTUSER1_ADM",
              "generalUsername" to "TESTUSER1_GEN",
              "defaultCaseloadId" to "BXI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `Link a General user to an existing Admin User`() {
      val createLinkedGeneralRequest = CreateLinkedGeneralUserRequest("TESTUSER1_ADM", "TESTUSER1_GEN", "BXI")

      nomisApiMockServer.stubCreateLinkedGeneralUser(createLinkedGeneralRequest)

      val prisonStaffUser = webTestClient.post().uri("/linkedprisonusers/general")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingAdminUsername" to "TESTUSER1_ADM",
              "generalUsername" to "TESTUSER1_GEN",
              "defaultCaseloadId" to "BXI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(PrisonStaffUserDto::class.java)
        .returnResult().responseBody!!

      assertThat(prisonStaffUser.staffId).isEqualTo(100L)
      assertThat(prisonStaffUser.firstName).isEqualTo("First")
      assertThat(prisonStaffUser.lastName).isEqualTo("Last")
      assertThat(prisonStaffUser.status).isEqualTo("ACTIVE")
      assertThat(prisonStaffUser.primaryEmail).isEqualTo("f.l@justice.gov.uk")
      assertThat(prisonStaffUser.generalAccount?.username).isEqualTo("TESTUSER1_GEN")
      assertThat(prisonStaffUser.generalAccount?.active).isEqualTo(false)
      assertThat(prisonStaffUser.generalAccount?.accountType).isEqualTo(PrisonUsageType.GENERAL)
      assertThat(prisonStaffUser.generalAccount?.activeCaseload?.id).isEqualTo("BXI")
      assertThat(prisonStaffUser.generalAccount?.activeCaseload?.name).isEqualTo("Brixton (HMP)")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(0)?.id ?: String).isEqualTo("NWEB")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(0)?.name ?: String).isEqualTo("Nomis-web Application")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(1)?.id ?: String).isEqualTo("BXI")
      assertThat(prisonStaffUser.generalAccount?.caseloads?.get(1)?.name ?: String).isEqualTo("Brixton (HMP)")
      assertThat(prisonStaffUser.adminAccount?.username).isEqualTo("TESTUSER1_ADM")
      assertThat(prisonStaffUser.adminAccount?.active).isEqualTo(false)
      assertThat(prisonStaffUser.adminAccount?.accountType).isEqualTo(PrisonUsageType.ADMIN)
      assertThat(prisonStaffUser.adminAccount?.activeCaseload?.id).isEqualTo("CADM_I")
      assertThat(prisonStaffUser.adminAccount?.activeCaseload?.name).isEqualTo("Central Administration Caseload For Hmps")
      assertThat(prisonStaffUser.adminAccount?.caseloads?.get(0)?.id ?: String).isEqualTo("NWEB")
      assertThat(prisonStaffUser.adminAccount?.caseloads?.get(0)?.name ?: String).isEqualTo("Nomis-web Application")
      assertThat(prisonStaffUser.adminAccount?.caseloads?.get(1)?.id ?: String).isEqualTo("CADM_I")
      assertThat(
        prisonStaffUser.adminAccount?.caseloads?.get(1)?.name ?: String,
      ).isEqualTo("Central Administration Caseload For Hmps")

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/link-general-account/${createLinkedGeneralRequest.existingAdminUsername}"))
          .withRequestBody(
            containing(
              """
              {"username":"${createLinkedGeneralRequest.generalUsername}","defaultCaseloadId":"${createLinkedGeneralRequest.defaultCaseloadId}"}
              """.trimIndent(),
            ),
          ),
      )
    }

    @Test
    fun `create linked general user returns error when the general user already exists`() {
      nomisApiMockServer.stubConflictOnPostTo("/users/link-general-account/TESTUSER1_ADM")
      webTestClient.post().uri("/linkedprisonusers/general")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingAdminUsername" to "TESTUSER1_ADM",
              "generalUsername" to "TESTUSER1_GEN",
              "defaultCaseloadId" to "BXI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(CONFLICT)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo(CONFLICT.value())
        .jsonPath("$.userMessage").isEqualTo("User test message")
        .jsonPath("$.developerMessage").isEqualTo("Developer test message")
    }

    @Test
    fun `create linked general user returns error when specified admin user is not found`() {
      nomisApiMockServer.stubNotFoundOnPostTo("/users/link-general-account/TESTUSER_ADM_NOT_FOUND")
      webTestClient.post().uri("/linkedprisonusers/general")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingAdminUsername" to "TESTUSER_ADM_NOT_FOUND",
              "generalUsername" to "TESTUSER_GEN",
              "defaultCaseloadId" to "BXI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(NOT_FOUND)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("status").isEqualTo(NOT_FOUND.value())
        .jsonPath("$.userMessage").isEqualTo("User test message")
        .jsonPath("$.developerMessage").isEqualTo("Developer test message")
    }

    @Test
    fun `create linked general user call passes through error when bad request error is thrown from nomisapi`() {
      nomisApiMockServer.stubSpecifiedHttpStatusOnPostTo("/users/link-general-account/TESTUSER1_ADM", BAD_REQUEST)
      webTestClient.post().uri("/linkedprisonusers/general")
        .headers(setAuthorisation(roles = listOf("ROLE_CREATE_USER")))
        .body(
          fromValue(
            mapOf(
              "existingAdminUsername" to "TESTUSER1_ADM",
              "generalUsername" to "TESTUSER1_GEN",
              "defaultCaseloadId" to "BXI",
            ),
          ),
        )
        .exchange()
        .expectStatus().isEqualTo(BAD_REQUEST)
        .expectBody()
        .jsonPath("status").isEqualTo(BAD_REQUEST.value())
        .jsonPath("$.userMessage").isEqualTo("User test message")
        .jsonPath("$.developerMessage").isEqualTo("Developer test message")
    }
  }

  @Nested
  inner class FindUsersByFirstAndLastName {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/prisonusers?firstName=First&lastName=Last")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri("/prisonusers?firstName=First&lastName=Last")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun ` prison user found success`() {
      nomisApiMockServer.stubFindUsersByFirstAndLastName("First", "Last")
      hmppsAuthMockServer.stubUserEmails()
      webTestClient.get().uri("/prisonusers?firstName=First&lastName=Last")
        .headers(setAuthorisation(roles = listOf("ROLE_USE_OF_FORCE")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("[*].username").value<List<String>> {
          assertThat(it).containsExactlyElementsOf(listOf("NUSER_GEN", "NUSER_ADM"))
        }
        .jsonPath("$.[0]").value<Map<String, Any>> {
          assertThat(it).containsAllEntriesOf(
            mapOf(
              "username" to "NUSER_GEN",
              "staffId" to 123456,
              "email" to "First.Last@digital.justice.gov.uk",
              "verified" to true,
              "firstName" to "First",
              "lastName" to "Last",
              "name" to "First Last",
              "activeCaseLoadId" to "MDI",
            ),
          )
        }
    }

    @Test
    fun `Prison user not found`() {
      nomisApiMockServer.stubGetWithEmptyReturn("/users/staff?firstName=not&lastName=found", HttpStatus.OK)

      webTestClient
        .get().uri("/prisonusers?firstName=not&lastName=found")
        .headers(setAuthorisation(roles = listOf("ROLE_STAFF_SEARCH")))
        .exchange()
        .expectStatus().isOk
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody()
    }
  }

  @Nested
  inner class FindUserByUsername {
    private val username = "NUSER_GEN"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/prisonusers/$username")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/prisonusers/$username")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri("/prisonusers/$username")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `user not found`() {
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      webTestClient.get().uri("/prisonusers/$username")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isNotFound

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo("/users/$username")),
      )
    }

    @Test
    fun `user not searched when username has @`() {
      val username = "USER@NAME"

      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      webTestClient.get().uri("/prisonusers/$username")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `find user by username`() {
      nomisApiMockServer.stubFindUserByUsername(username)
      val prisonUser = webTestClient.get().uri("/prisonusers/$username")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody(NewPrisonUserDto::class.java)
        .returnResult().responseBody!!

      assertThat(prisonUser.username).isEqualTo(username)
      assertThat(prisonUser.firstName).isEqualTo("Nomis")
      assertThat(prisonUser.lastName).isEqualTo("Take")
      assertThat(prisonUser.primaryEmail).isEqualTo("nomis.usergen@digital.justice.gov.uk")

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo("/users/$username")),
      )
    }
  }

  @Nested
  inner class FindUserDetailsByUsername {
    private val username = "NUSER_GEN"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri("/prisonusers/$username/details")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/prisonusers/$username/details")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri("/prisonusers/$username/details")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `user not found`() {
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      webTestClient.get().uri("/prisonusers/$username/details")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isNotFound

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo("/users/$username")),
      )
    }

    @Test
    fun `user not searched when username has @`() {
      val username = "USER@NAME"

      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      webTestClient.get().uri("/prisonusers/$username/details")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `find user by username`() {
      nomisApiMockServer.stubFindUserByUsername(username)
      val prisonUserDetails = webTestClient.get().uri("/prisonusers/$username/details")
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk
        .expectBody(PrisonUserDetails::class.java)
        .returnResult().responseBody!!

      assertThat(prisonUserDetails.username).isEqualTo(username)
      assertThat(prisonUserDetails.firstName).isEqualTo("Nomis")
      assertThat(prisonUserDetails.lastName).isEqualTo("Take")
      assertThat(prisonUserDetails.primaryEmail).isEqualTo("nomis.usergen@digital.justice.gov.uk")
      assertThat(prisonUserDetails.active).isEqualTo(true)

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo("/users/$username")),
      )
    }
  }

  @Nested
  inner class EnableUser {
    private val username = "NUSER_GEN"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/prisonusers/$username/enable-user")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/prisonusers/$username/enable-user")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.put().uri("/prisonusers/$username/enable-user")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `user not found`() {
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      webTestClient.put().uri("/prisonusers/$username/enable-user")
        .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_NOMIS_USER_ACCOUNT")))
        .exchange()
        .expectStatus().isNotFound

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo("/users/$username")),
      )
    }

    @Test
    fun `enable user calls unlock-user endpoint`() {
      nomisApiMockServer.stubFindUserByUsernameNoEmail(username)
      nomisApiMockServer.stubPut("/users/$username/unlock-user", OK)
      webTestClient.put().uri("/prisonusers/$username/enable-user")
        .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_NOMIS_USER_ACCOUNT")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(
        putRequestedFor(urlEqualTo("/users/$username/unlock-user")),
      )
    }
  }

  @Nested
  inner class DisableUser {
    private val username = "NUSER_GEN"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/prisonusers/$username/disable-user")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/prisonusers/$username/disable-user")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.put().uri("/prisonusers/$username/disable-user")
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `user not found`() {
      nomisApiMockServer.stubGetFail("/users/$username", NOT_FOUND)
      webTestClient.put().uri("/prisonusers/$username/disable-user")
        .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_NOMIS_USER_ACCOUNT")))
        .exchange()
        .expectStatus().isNotFound

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo("/users/$username")),
      )
    }

    @Test
    fun `disable user calls lock-user endpoint`() {
      nomisApiMockServer.stubFindUserByUsernameNoEmail(username)
      nomisApiMockServer.stubPut("/users/$username/lock-user", OK)
      webTestClient.put().uri("/prisonusers/$username/disable-user")
        .headers(setAuthorisation(roles = listOf("ROLE_MANAGE_NOMIS_USER_ACCOUNT")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(
        putRequestedFor(urlEqualTo("/users/$username/lock-user")),
      )
    }
  }

  @Nested
  inner class FindUsersByFilter {
    private val localUri =
      "/prisonusers/search?nameFilter=admin"
    private val nomisUri =
      "/users?nameFilter=admin&page=0&size=10&sort=lastName,ASC&sort=firstName,ASC"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri(localUri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `findUsersWithFilter calls find-users endpoint`() {
      nomisApiMockServer.stubFindUsersByFilter(nomisUri, OK)
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo(nomisUri)),
      )
    }

    @Test
    fun `findUsersWithFilter calls find-users endpoint using service client`() {
      nomisApiMockServer.stubFindUsersByFilter(nomisUri, OK)
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_STAFF_SEARCH")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo(nomisUri)),
      )
    }
  }

  @Nested
  inner class FindUsersByCaseloadAndRole {
    private val localUri =
      "/prisonusers/find-by-caseload-and-role?activeCaseload=BXI&roleCode=ADD_SENSITIVE_CASE_NOTES"
    private val nomisUri =
      "/users?activeCaseload=BXI&accessRoles=ADD_SENSITIVE_CASE_NOTES&page=0&size=10&sort=lastName,ASC&sort=firstName,ASC"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri(localUri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `findUsersByCaseloadAndRole calls find-users endpoint`() {
      nomisApiMockServer.stubFindUsersByFilter(nomisUri, OK)
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_USERS__PRISON_USERS__FIND_BY_CASELOAD_AND_ROLE__RO")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo(nomisUri)),
      )
    }

    @Test
    fun `findUsersByCaseloadAndRole calls find-users endpoint with status`() {
      val localUri =
        "/prisonusers/find-by-caseload-and-role?activeCaseload=BXI&roleCode=ADD_SENSITIVE_CASE_NOTES&status=ACTIVE"
      val nomisUri =
        "/users?status=ACTIVE&activeCaseload=BXI&accessRoles=ADD_SENSITIVE_CASE_NOTES&page=0&size=10&sort=lastName,ASC&sort=firstName,ASC"

      nomisApiMockServer.stubFindUsersByFilter(nomisUri, OK)
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_USERS__PRISON_USERS__FIND_BY_CASELOAD_AND_ROLE__RO")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo(nomisUri)),
      )
    }

    @Test
    fun `findUsersByCaseloadAndRole fails if activeCaseload not defined`() {
      webTestClient.get().uri("/prisonusers/find-by-caseload-and-role?nomisRole=123")
        .headers(setAuthorisation(roles = listOf("ROLE_USERS__PRISON_USERS__FIND_BY_CASELOAD_AND_ROLE__RO")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `findUsersByCaseloadAndRole fails if roleCode not defined`() {
      webTestClient.get().uri("/prisonusers/find-by-caseload-and-role?activeCaseload=BXI")
        .headers(setAuthorisation(roles = listOf("ROLE_USERS__PRISON_USERS__FIND_BY_CASELOAD_AND_ROLE__RO")))
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  inner class DownloadUsersByFilter {
    private val localUri =
      "/prisonusers/download?nameFilter=admin"
    private val nomisUri =
      "/users/download?nameFilter=admin"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri(localUri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `downloadUsersWithFilter calls download-users endpoint`() {
      nomisApiMockServer.stubDownloadUsersByFilter(nomisUri, OK)
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo(nomisUri)),
      )
    }
  }

  @Nested
  inner class DownloadAdminUsersByFilter {
    private val localUri =
      "/prisonusers/download/admins?nameFilter=admin"
    private val nomisUri =
      "/users/download/admins?nameFilter=admin"

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.get().uri(localUri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_WRONG_ROLE")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `downloadUsersWithFilter calls download-users endpoint`() {
      nomisApiMockServer.stubDownloadAdminUsersByFilter(nomisUri, OK)
      webTestClient.get().uri(localUri)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_ACCESS_ROLES_ADMIN")))
        .exchange()
        .expectStatus().isOk

      nomisApiMockServer.verify(
        getRequestedFor(urlEqualTo(nomisUri)),
      )
    }
  }
}

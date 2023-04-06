package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromValue
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageusersapi.model.UsageType

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
  }

  @Nested
  inner class CreateLinkedAdminUser {
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
    fun `create Linked Central Admin user`() {
      val createLinkedAdminUserRequest = CreateLinkedAdminUserRequest("TEST_USER", "TEST_USER_ADM")

      nomisApiMockServer.stubCreateLinkedCentralAdminUser(createLinkedAdminUserRequest)

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
      assertThat(prisonStaffUser.generalAccount?.accountType).isEqualTo(UsageType.GENERAL)
      assertThat(prisonStaffUser.adminAccount?.accountType).isEqualTo(UsageType.ADMIN)

      nomisApiMockServer.verify(
        postRequestedFor(urlEqualTo("/users/link-admin-account/${createLinkedAdminUserRequest.existingUsername}"))
          .withRequestBody(
            containing(
              """
              {"username":"${createLinkedAdminUserRequest.adminUsername}"}
              """.trimIndent(),
            ),
          ),
      )
    }
  }

  @Nested
  inner class CreateUserError {
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
  }

  @Nested
  inner class CreateLinkedAdminUserError {
    @Test
    fun `create linked central admin user returns error when general user already has a linked admin account`() {
      nomisApiMockServer.stubCreateLinkedCentralAdminUserDuplicateConflict()
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
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("User already exists: Admin user already exists for this staff member")
          assertThat(it["developerMessage"] as String).isEqualTo("Admin user already exists for this staff member")
        }
    }

    @Test
    fun `create linked central admin user returns error when the admin user already exists`() {
      nomisApiMockServer.stubCreateLinkedCentralAdminUserExistConflict()
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
        .jsonPath("status").isEqualTo("409")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("User already exists: User TEST_USER_ADM already exists")
          assertThat(it["developerMessage"] as String).isEqualTo("User TEST_USER_ADM already exists")
        }
    }

    @Test
    fun `create linked central admin user returns error when specified general user is not found`() {
      nomisApiMockServer.stubCreateLinkedCentralAdminWhenGeneralUserNotFound()
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
        .jsonPath("status").isEqualTo("404")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("User not found: Linked User Account TEST_USER_NOT_FOUND not found")
          assertThat(it["developerMessage"] as String).isEqualTo("Linked User Account TEST_USER_NOT_FOUND not found")
        }
    }

    @Test
    fun `create linked central admin user passes through error when error thrown from nomisapi`() {
      nomisApiMockServer.stubCreateLinkedCentralAdminUserWithErrorFail(BAD_REQUEST)

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
        .jsonPath("status").isEqualTo("400")
        .jsonPath("$").value<Map<String, Any>> {
          assertThat(it["userMessage"] as String).isEqualTo("Validation failure: General user name is required")
          assertThat(it["developerMessage"] as String).isEqualTo("A bigger message")
        }
    }
  }

  @Nested
  inner class CreateUserMissingFieldValidation {
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
}

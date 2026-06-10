package uk.gov.justice.digital.hmpps.manageusersapi.resource.bulkjob

import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.manageusersapi.config.SqsConfig
import uk.gov.justice.digital.hmpps.manageusersapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobStatus
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit.SECONDS
import java.util.stream.Stream

@Import(SqsConfig::class)
class BulkJobsControllerIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var bulkUserJobRepository: BulkUserJobRepository

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  internal val auditQueue by lazy { hmppsQueueService.findByQueueId("bulkuserjobqueue") as HmppsQueue }

  companion object {
    @JvmStatic
    fun createBulkJobInvalidScenarios(): Stream<Arguments> = Stream.of(
      Arguments.of(MultipartBuilder(), "Required part 'userCsv' is missing"),
      Arguments.of(MultipartBuilder().usersCsv(), "Required part 'bulkJobDetails' is missing"),
      Arguments.of(MultipartBuilder().usersCsv(filename = "users.txt").bulkJobDetailsJson(), "Validation failure: Uploaded users file is not a CSV file"),
      Arguments.of(MultipartBuilder().usersCsv(content = "").bulkJobDetailsJson(), "Validation failure: Uploaded users file is empty"),
      Arguments.of(MultipartBuilder().usersCsv(content = "USER123,USER654").bulkJobDetailsJson(), "Validation failure: Users csv row does not have exactly 1 column"),
      Arguments.of(MultipartBuilder().usersCsv().bulkJobDetailsJson("{\"jiraReference\":\"JIRA\"}"), "Validation failure: roles must be supplied"),
      Arguments.of(MultipartBuilder().usersCsv().bulkJobDetailsJson("{\"roles\":[\"ROLE123\"]}"), "Validation failure: jiraReference must be supplied"),
      Arguments.of(MultipartBuilder().usersCsv().bulkJobDetailsJson("{\"jiraReference\":\"JIR\",\"roles\":[\"ROLE123\"]}"), "Validation failure: jiraReference must be between 4 and 266 characters"),
      Arguments.of(MultipartBuilder().usersCsv().bulkJobDetailsJson("{\"jiraReference\":\"${"J".repeat(267)}\",\"roles\":[\"ROLE123\"]}"), "Validation failure: jiraReference must be between 4 and 266 characters"),
      Arguments.of(MultipartBuilder().usersCsv().bulkJobDetailsJson("{\"jiraReference\":\"JIRA\",\"roles\":[]}"), "Validation failure: roles must be supplied"),
    )

    private fun MultipartBuilder.usersCsv(content: String = "USER123\nUSER654", filename: String = "users.csv"): MultipartBuilder {
      this.addPart("userCsv", ByteArrayResource(content.toByteArray()), filename)
      return this
    }

    private fun MultipartBuilder.bulkJobDetailsJson(
      content: String = """
                  {
                    "jiraReference": "JIRA-1234",
                    "roles": ["ROLE_ONE","ROLE_FOUR"]
                  }""",
    ): MultipartBuilder {
      this.addPart(
        "bulkJobDetails",
        HttpEntity(content, HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }),
      )
      return this
    }
  }

  @Nested
  open inner class CreateBulkUserAdditionsJob {
    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/bulk-jobs/user-role-additions")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(buildValidMultipart())
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/bulk-jobs/user-role-additions")
        .headers(setAuthorisation(roles = listOf()))
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(buildValidMultipart())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden when wrong role`() {
      webTestClient.post().uri("/bulk-jobs/user-role-additions")
        .headers(setAuthorisation(roles = listOf("ROLE_AUDIT")))
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(buildValidMultipart())
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("uk.gov.justice.digital.hmpps.manageusersapi.resource.bulkjob.BulkJobsControllerIntTest#createBulkJobInvalidScenarios")
    open fun `bad request when invalid input`(multipart: MultipartBuilder, expectedMessage: String) {
      webTestClient.post().uri("/bulk-jobs/user-role-additions")
        .headers(setAuthorisation(user = "TEST_USR", roles = listOf("ROLE_MANAGE_USER_BULK_JOBS")))
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(multipart.build())
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").isEqualTo(expectedMessage)
    }

    @Transactional
    @Test
    open fun `bulk user additions job accepted`() {
      val response = webTestClient.post().uri("/bulk-jobs/user-role-additions")
        .headers(setAuthorisation(user = "TEST_USR", roles = listOf("ROLE_MANAGE_USER_BULK_JOBS")))
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(buildValidMultipart())
        .exchange()
        .expectStatus().isAccepted
        .returnResult(BulkUserRoleAdditionsResponse::class.java)
        .responseBody.blockFirst()

      assertThat(response).isNotNull
      val bulkJob = bulkUserJobRepository.findById(response!!.id)
      assertThat(bulkJob).isPresent.hasValueSatisfying {
        assertThat(it).usingRecursiveComparison().ignoringFields("jobItems", "requestDateTime").isEqualTo(
          BulkUserJob(response.id, "JIRA-1234", BulkUserJobStatus.PENDING, "TEST_USR"),
        )
        assertThat(it.requestDateTime).isCloseTo(LocalDateTime.now(ZoneId.systemDefault()), within(5, SECONDS))
        assertThat(it.jobItems).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id").containsExactlyInAnyOrder(
          BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.CREATED, bulkUserJob = it),
          BulkUserJobItem(username = "USER654", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.CREATED, bulkUserJob = it),
          BulkUserJobItem(username = "USER123", rolename = "ROLE_FOUR", status = BulkUserJobItemStatus.CREATED, bulkUserJob = it),
          BulkUserJobItem(username = "USER654", rolename = "ROLE_FOUR", status = BulkUserJobItemStatus.CREATED, bulkUserJob = it),
        )
      }
      assertThat(auditQueue.sqsClient.countAllMessagesOnQueue(auditQueue.queueUrl).get()).isEqualTo(1)
    }

    private fun buildValidMultipart(): BodyInserters.MultipartInserter = MultipartBuilder().usersCsv().bulkJobDetailsJson().build()
  }
}

class MultipartBuilder {

  private val multipartBodyBuilder = MultipartBodyBuilder()

  fun addPart(name: String, content: Any, filename: String? = null): MultipartBuilder {
    val part = multipartBodyBuilder.part(name, content)
    if (filename != null) {
      part.filename(filename)
    }
    return this
  }

  fun build(): BodyInserters.MultipartInserter = BodyInserters.fromMultipartData(multipartBodyBuilder.build())
}

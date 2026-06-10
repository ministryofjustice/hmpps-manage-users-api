package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.mock.web.MockMultipartFile
import uk.gov.justice.digital.hmpps.manageusersapi.event.BulkJobPublisher
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus.CREATED
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobStatus
import uk.gov.justice.digital.hmpps.manageusersapi.resource.bulkjob.BulkUserRoleAdditionsRequest
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit.SECONDS
import java.util.UUID

class BulkUserJobServiceTest {
  private val bulkUserJobRepository: BulkUserJobRepository = mock()
  private val bulkJobPublisher: BulkJobPublisher = mock()
  private val bulkUserJobCaptor = argumentCaptor<BulkUserJob>()
  private val bulkUserJobService = BulkUserJobService(bulkUserJobRepository, bulkJobPublisher)
  private var jiraReference: String = "JIRA-123"
  private var roles: List<String> = listOf("ROLE_ONE", "ROLE_TWO")

  @Test
  fun `Bulk user role additions job can be created`() {
    whenCreateBulkUserRoleAdditionsJobWithCsvContent("USER123\n  USER456  \nUSER789 ".toByteArray())

    verify(bulkUserJobRepository).save(bulkUserJobCaptor.capture())
    val bulkUserJob = bulkUserJobCaptor.firstValue
    assertThat(bulkUserJob).usingRecursiveComparison().ignoringFields("id", "requestDateTime", "jobItems").isEqualTo(
      BulkUserJob(
        jiraReference = "JIRA-123",
        status = BulkUserJobStatus.PENDING,
        requestedBy = "userone",
      ),
    )
    assertThat(bulkUserJob.id).isNotNull()
    assertThat(bulkUserJob.requestDateTime).isCloseTo(LocalDateTime.now(ZoneId.systemDefault()), within(5, SECONDS))
    assertThat(bulkUserJob.jobItems).usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
      .containsExactlyInAnyOrder(
        BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = CREATED, bulkUserJob = bulkUserJob),
        BulkUserJobItem(username = "USER123", rolename = "ROLE_TWO", status = CREATED, bulkUserJob = bulkUserJob),
        BulkUserJobItem(username = "USER456", rolename = "ROLE_ONE", status = CREATED, bulkUserJob = bulkUserJob),
        BulkUserJobItem(username = "USER456", rolename = "ROLE_TWO", status = CREATED, bulkUserJob = bulkUserJob),
        BulkUserJobItem(username = "USER789", rolename = "ROLE_ONE", status = CREATED, bulkUserJob = bulkUserJob),
        BulkUserJobItem(username = "USER789", rolename = "ROLE_TWO", status = CREATED, bulkUserJob = bulkUserJob),
      ).allSatisfy { assertThat(it.status).isNotNull() }
    verify(bulkJobPublisher).publishBulkUserJobEvent(bulkUserJob)
  }

  @Test
  fun `Bulk user role additions validation error when no data`() {
    assertThatThrownBy { whenCreateBulkUserRoleAdditionsJobWithCsvContent("".toByteArray()) }
      .isInstanceOf(ValidationException::class.java)
      .hasMessage("Users csv does not contain any rows")
  }

  @ParameterizedTest
  @ValueSource(strings = ["USER123,USER456", "USER123\nUSER456,USER789"])
  fun `Bulk user role additions validation error when not exactly one column`(csvContent: String) {
    assertThatThrownBy { whenCreateBulkUserRoleAdditionsJobWithCsvContent(csvContent.toByteArray()) }
      .isInstanceOf(ValidationException::class.java)
      .hasMessage("Users csv row does not have exactly 1 column")
  }

  private fun whenCreateBulkUserRoleAdditionsJobWithCsvContent(contentBytes: ByteArray): UUID = bulkUserJobService
    .createBulkUserRoleAdditionsJob(
      MockMultipartFile("users.csv", contentBytes),
      BulkUserRoleAdditionsRequest(jiraReference, roles),
      "userone",
    )
}

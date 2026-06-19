package uk.gov.justice.digital.hmpps.manageusersapi.resource.bulkjob

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.Authentication
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobStatus
import uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob.BulkUserJobService
import java.time.LocalDateTime
import java.util.UUID

class BulkJobsControllerTest {

  private val bulkUserJobService: BulkUserJobService = mock()
  private val authentication: Authentication = mock()
  private val bulkJobsController = BulkJobsController(bulkUserJobService)

  @Nested
  inner class CreateUserRoleAdditionsJob {

    @Test
    fun `Create bulk user additions job`() {
      val bulkJobId = UUID.randomUUID()
      val usersCsv =
        MockMultipartFile("user.csv", "user.csv", MediaType.MULTIPART_FORM_DATA_VALUE, "USER_123".toByteArray())
      val bulkJobDetails = BulkUserRoleAdditionsRequest("JIRA-123", listOf("ROLE-123"))
      whenever(authentication.name).thenReturn("user-abc")
      whenever(bulkUserJobService.createBulkUserRoleAdditionsJob(any(), any(), any())).thenReturn(bulkJobId)

      val response = bulkJobsController.createUserRoleAdditionsJob(usersCsv, bulkJobDetails, authentication)

      assertThat(response)
        .returns(HttpStatus.ACCEPTED, ResponseEntity<BulkUserRoleAdditionsResponse>::getStatusCode)
        .returns(BulkUserRoleAdditionsResponse(bulkJobId), ResponseEntity<BulkUserRoleAdditionsResponse>::getBody)
      verify(bulkUserJobService).createBulkUserRoleAdditionsJob(usersCsv, bulkJobDetails, "user-abc")
    }

    @Test
    fun `Validation exception when not csv file`() {
      val usersCsv =
        MockMultipartFile("user.jpg", "user.jpg", MediaType.MULTIPART_FORM_DATA_VALUE, "content".toByteArray())
      val bulkJobDetails = BulkUserRoleAdditionsRequest("JIRA-123", listOf("ROLE-123"))
      assertThatThrownBy { bulkJobsController.createUserRoleAdditionsJob(usersCsv, bulkJobDetails, authentication) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessage("Uploaded users file is not a CSV file")
    }

    @Test
    fun `Validation exception when csv file is empty`() {
      val usersCsv =
        MockMultipartFile("user.csv", "user.csv", MediaType.MULTIPART_FORM_DATA_VALUE, null)
      val bulkJobDetails = BulkUserRoleAdditionsRequest("JIRA-123", listOf("ROLE-123"))
      assertThatThrownBy { bulkJobsController.createUserRoleAdditionsJob(usersCsv, bulkJobDetails, authentication) }
        .isInstanceOf(ValidationException::class.java)
        .hasMessage("Uploaded users file is empty")
    }
  }

  @Nested
  inner class GetUserRoleAdditionsJobs {
    private val bulkJobs = listOf(
      BulkUserJob(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        status = BulkUserJobStatus.PENDING,
        jiraReference = "ABC-123",
        requestedBy = "Test",
        requestDateTime = LocalDateTime.parse("2026-06-01T11:11:11"),
      ),
      BulkUserJob(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        status = BulkUserJobStatus.COMPLETE,
        jiraReference = "DEF-456",
        requestedBy = "TestTwo",
        requestDateTime = LocalDateTime.parse("2026-06-02T12:12:12"),
      ),
    )
    val expectedResults = listOf(
      BulkUserRoleAdditionsJobSummary(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        jiraReference = "ABC-123",
        status = "PENDING",
        requestedBy = "Test",
        requestDateTime = LocalDateTime.parse("2026-06-01T11:11:11"),
      ),
      BulkUserRoleAdditionsJobSummary(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        jiraReference = "DEF-456",
        status = "COMPLETE",
        requestedBy = "TestTwo",
        requestDateTime = LocalDateTime.parse("2026-06-02T12:12:12"),
      ),
    )

    @Test
    fun `Can get bulk user role additions jobs with search and pagination parameters if specified in controller`() {
      whenever(bulkUserJobService.getBulkUserRoleAdditionsJobs("testSearchString", 0, 1)).thenReturn(bulkJobs)

      val result = bulkJobsController.getUserRoleAdditionsJobs("testSearchString", 0, 1)

      assertThat(result).isEqualTo(expectedResults)
      verify(bulkUserJobService).getBulkUserRoleAdditionsJobs("testSearchString", 0, 1)
    }

    @Test
    fun `Can get bulk user role additions jobs with no search or pagination parameters if unspecified in controller`() {
      whenever(bulkUserJobService.getBulkUserRoleAdditionsJobs("", null, null)).thenReturn(bulkJobs)

      val result = bulkJobsController.getUserRoleAdditionsJobs("", null, null)

      assertThat(result).isEqualTo(expectedResults)
      verify(bulkUserJobService).getBulkUserRoleAdditionsJobs("", null, null)
    }
  }
}

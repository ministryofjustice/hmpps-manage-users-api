package uk.gov.justice.digital.hmpps.manageusersapi.resource.bulkjob

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
import uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob.BulkUserJobService
import java.util.UUID

class BulkJobsControllerTest {

  private val bulkUserJobService: BulkUserJobService = mock()
  private val authentication: Authentication = mock()
  private val bulkJobsController = BulkJobsController(bulkUserJobService)

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

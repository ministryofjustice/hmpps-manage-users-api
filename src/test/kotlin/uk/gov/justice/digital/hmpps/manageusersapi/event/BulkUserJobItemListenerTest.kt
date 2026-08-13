package uk.gov.justice.digital.hmpps.manageusersapi.event

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob.BulkUserJobItemRoleAssignmentService
import java.util.UUID

class BulkUserJobItemListenerTest {
  private val bulkUserJobItemRoleAssignmentService: BulkUserJobItemRoleAssignmentService = mock()
  private val listener = BulkUserJobItemListener(bulkUserJobItemRoleAssignmentService)

  @Test
  fun `should delegate message processing to role assignment service`() {
    val message = BulkUserJobItemMessage(
      jobId = UUID.randomUUID(),
      jobItemId = UUID.randomUUID(),
      username = "USER123",
      rolename = "ROLE_ONE",
      jiraReference = "JIRA-123",
      requestedBy = "userabc",
    )

    listener.onBulkUserJobItemMessage(message)

    verify(bulkUserJobItemRoleAssignmentService).processRoleAssignmentMessage(message)
  }
}

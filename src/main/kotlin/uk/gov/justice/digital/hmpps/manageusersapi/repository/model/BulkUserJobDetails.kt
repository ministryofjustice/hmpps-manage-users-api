package uk.gov.justice.digital.hmpps.manageusersapi.repository.model

import java.time.LocalDateTime
import java.util.UUID

data class BulkUserJobDetails(
  val id: UUID,
  val jiraReference: String,
  val status: BulkUserJobStatus,
  val requestedBy: String,
  val requestDateTime: LocalDateTime,
  val totalCount: Long = 0,
  val successCount: Long = 0,
  val errorCount: Long = 0,
)

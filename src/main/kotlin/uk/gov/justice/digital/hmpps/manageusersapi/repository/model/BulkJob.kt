package uk.gov.justice.digital.hmpps.manageusersapi.repository.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Entity
@Table(name = "BULK_JOB")
data class BulkJob(

  @Id
  val id: UUID = UUID.randomUUID(),
  val jiraReference: String,
  val numberOfRoles: Int,
  val numberOfUsers: Int,
  @Enumerated(EnumType.STRING)
  val status: Status = Status.Pending,
  val requestedBy: String,
  val requestDateTime: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),
)

enum class Status {
  Pending,
  Completed,
  Errored,
}

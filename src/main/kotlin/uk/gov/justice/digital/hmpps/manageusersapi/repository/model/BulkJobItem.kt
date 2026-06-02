package uk.gov.justice.digital.hmpps.manageusersapi.repository.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "BULK_JOB_ITEM")
data class BulkJobItem(

  @Id
  val id: UUID = UUID.randomUUID(),
  val username: String,
  val rolename: String,
  @Enumerated(EnumType.STRING)
  var status: BulkJobItemStatus = BulkJobItemStatus.Created,
  val result: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bulk_job_id", referencedColumnName = "id", nullable = false)
  val bulkJob: BulkJob,
) {
  fun complete(): BulkJobItem {
    this.status = BulkJobItemStatus.Completed
    return this
  }

  fun pending(): BulkJobItem {
    this.status = BulkJobItemStatus.Pending
    return this
  }
}

enum class BulkJobItemStatus {
  Created,
  Pending,
  Completed,
  Errored,
}

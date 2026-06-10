package uk.gov.justice.digital.hmpps.manageusersapi.repository.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Entity
@Table(name = "BULK_USER_JOB")
class BulkUserJob(

  @Id
  val id: UUID = UUID.randomUUID(),
  val jiraReference: String,
  @Enumerated(EnumType.STRING)
  val status: BulkUserJobStatus = BulkUserJobStatus.PENDING,
  val requestedBy: String,
  val requestDateTime: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),

  @OneToMany(mappedBy = "bulkUserJob", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  val jobItems: MutableList<BulkUserJobItem> = mutableListOf(),
) {
  fun addItem(user: String, role: String) {
    jobItems.add(
      BulkUserJobItem(
        username = user,
        rolename = role,
        bulkUserJob = this,
      ),
    )
  }

  override fun equals(other: Any?): Boolean = this === other || (other is BulkUserJob && id == other.id)

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "BulkUserJob(id=$id, jiraReference='$jiraReference', status=$status, requestedBy='$requestedBy', requestDateTime=$requestDateTime)"
}

enum class BulkUserJobStatus {
  PENDING,
  COMPLETE,
}

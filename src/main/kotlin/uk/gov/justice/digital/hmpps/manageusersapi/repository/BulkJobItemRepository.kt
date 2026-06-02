package uk.gov.justice.digital.hmpps.manageusersapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkJobItemStatus
import java.util.UUID

@Repository
interface BulkJobItemRepository : JpaRepository<BulkJobItem, UUID> {
  fun findAllByBulkJobIdAndStatusIs(bulkJobId: UUID, status: BulkJobItemStatus): List<BulkJobItem>
  fun countByBulkJobIdAndStatusIs(bulkJobId: UUID, status: BulkJobItemStatus): Long
  fun countByBulkJobId(bulkJobId: UUID): Long
}

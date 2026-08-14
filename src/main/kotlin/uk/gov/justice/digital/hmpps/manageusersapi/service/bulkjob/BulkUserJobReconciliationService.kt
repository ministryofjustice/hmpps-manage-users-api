package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class BulkUserJobReconciliationService(
  private val bulkUserJobItemRepository: BulkUserJobItemRepository,
  private val bulkUserJobRepository: BulkUserJobRepository,
  private val bulkUserJobItemService: BulkUserJobItemService,
  @Value("\${application.bulk-jobs.stale-claimed-threshold}") private val staleClaimedThreshold: Duration,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun reconcileBulkJob(jobId: UUID) {
    republishStaleClaimedItems(jobId)
    val updatedRows = bulkUserJobRepository.markCompleteIfAllItemsTerminal(jobId)
    if (updatedRows == 1) {
      log.info("Marked bulk user job {} as COMPLETE", jobId)
    }
  }

  private fun republishStaleClaimedItems(jobId: UUID) {
    val staleClaimCutoff = Instant.now().minus(staleClaimedThreshold)
    val staleClaimedItems = bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(
      jobId = jobId,
      status = BulkUserJobItemStatus.CLAIMED,
      claimedAt = staleClaimCutoff,
    )
    if (staleClaimedItems.isEmpty()) return

    val job = bulkUserJobRepository.findWithJobItemsById(jobId)
      .orElseThrow { IllegalStateException("Bulk user job $jobId not found when republishing stale claimed items") }

    staleClaimedItems.forEach { staleItem ->
      try {
        log.warn("Re-publishing stale claimed bulk user job item {} for job {}", staleItem.id, jobId)
        bulkUserJobItemService.republishStaleClaimedItem(job, staleItem)
      } catch (e: Exception) {
        log.error(
          "Failed to re-publish stale claimed bulk user job item {}; it remains CLAIMED and will be retried",
          staleItem.id,
          e,
        )
      }
    }
  }
}

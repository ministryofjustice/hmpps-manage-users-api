package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
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
    republishUnprocessedCreatedItems(jobId)
    republishUnprocessedPublishedItems(jobId)
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

  private fun republishUnprocessedCreatedItems(jobId: UUID) {
    val staleCreatedCutoff = LocalDateTime.now(ZoneId.systemDefault()).minus(staleClaimedThreshold)
    val createdItems = bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndJobRequestedBefore(
      jobId = jobId,
      status = BulkUserJobItemStatus.CREATED,
      requestedBefore = staleCreatedCutoff,
    )
    if (createdItems.isEmpty()) return

    val job = bulkUserJobRepository.findWithJobItemsById(jobId)
      .orElseThrow { IllegalStateException("Bulk user job $jobId not found when processing unprocessed created items") }

    createdItems.forEach { createdItem ->
      try {
        log.warn("Processing unpublished (CREATED) bulk user job item {} for job {}", createdItem.id, jobId)
        bulkUserJobItemService.processJobItem(job, createdItem)
      } catch (e: Exception) {
        log.error(
          "Failed to process unpublished bulk user job item {}; it remains CREATED and will be retried",
          createdItem.id,
          e,
        )
      }
    }
  }

  private fun republishUnprocessedPublishedItems(jobId: UUID) {
    val stalePublishedCutoff = LocalDateTime.now(ZoneId.systemDefault()).minus(staleClaimedThreshold)
    val publishedItems = bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndJobRequestedBefore(
      jobId = jobId,
      status = BulkUserJobItemStatus.PUBLISHED,
      requestedBefore = stalePublishedCutoff,
    )
    if (publishedItems.isEmpty()) return

    val job = bulkUserJobRepository.findWithJobItemsById(jobId)
      .orElseThrow { IllegalStateException("Bulk user job $jobId not found when re-publishing stale published items") }

    publishedItems.forEach { publishedItem ->
      try {
        log.warn("Re-publishing unprocessed (PUBLISHED) bulk user job item {} for job {}", publishedItem.id, jobId)
        bulkUserJobItemService.republishPublishedItem(job, publishedItem)
      } catch (e: Exception) {
        log.error(
          "Failed to re-publish unprocessed bulk user job item {}; it remains PUBLISHED and will be retried",
          publishedItem.id,
          e,
        )
      }
    }
  }
}

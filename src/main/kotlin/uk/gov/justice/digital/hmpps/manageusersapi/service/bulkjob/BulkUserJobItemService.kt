package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.event.BulkUserJobItemPublisher
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import java.time.Instant
import java.util.UUID

@Service
class BulkUserJobItemService(
  private val bulkUserJobItemRepository: BulkUserJobItemRepository,
  private val bulkUserJobItemPublisher: BulkUserJobItemPublisher,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun processJobItem(job: BulkUserJob, item: BulkUserJobItem) {
    val claimedAt = claimJobItem(item.id)
    if (claimedAt == null) {
      log.info("Skipping bulk user job item {} because it has already been processed", item.id)
      return
    }
    item.claimedAt = claimedAt

    try {
      bulkUserJobItemPublisher.publishBulkUserJobItemEvent(job, item)
    } catch (e: Exception) {
      releaseClaim(item.id)
      throw e
    }

    markPublished(item.id)
  }

  fun republishStaleClaimedItem(job: BulkUserJob, item: BulkUserJobItem) {
    val originalClaimedAt = item.claimedAt ?: Instant.EPOCH
    val refreshed = bulkUserJobItemRepository.updateStatusIfCurrent(
      jobItemId = item.id,
      currentStatus = BulkUserJobItemStatus.CLAIMED,
      newStatus = BulkUserJobItemStatus.CLAIMED,
      claimedAt = Instant.now(),
    ) == 1
    if (!refreshed) {
      log.info("Skipping stale re-publish of bulk user job item {} because it is no longer CLAIMED", item.id)
      return
    }

    try {
      bulkUserJobItemPublisher.publishBulkUserJobItemEvent(job, item)
    } catch (e: Exception) {
      bulkUserJobItemRepository.updateStatusIfCurrent(
        jobItemId = item.id,
        currentStatus = BulkUserJobItemStatus.CLAIMED,
        newStatus = BulkUserJobItemStatus.CLAIMED,
        claimedAt = originalClaimedAt,
      )
      throw e
    }

    val published = bulkUserJobItemRepository.updateStatusIfCurrent(
      jobItemId = item.id,
      currentStatus = BulkUserJobItemStatus.CLAIMED,
      newStatus = BulkUserJobItemStatus.PUBLISHED,
    ) == 1
    if (!published) {
      log.warn("Re-published stale bulk user job item {} but could not mark it PUBLISHED as its status changed concurrently", item.id)
    }
  }

  private fun claimJobItem(jobItemId: UUID): Instant? {
    val claimedAt = Instant.now()
    val updatedRows = bulkUserJobItemRepository.updateStatusIfCurrent(
      jobItemId = jobItemId,
      currentStatus = BulkUserJobItemStatus.CREATED,
      newStatus = BulkUserJobItemStatus.CLAIMED,
      claimedAt = claimedAt,
    )
    return if (updatedRows == 1) claimedAt else null
  }

  private fun releaseClaim(jobItemId: UUID) {
    bulkUserJobItemRepository.updateStatusIfCurrent(
      jobItemId = jobItemId,
      currentStatus = BulkUserJobItemStatus.CLAIMED,
      newStatus = BulkUserJobItemStatus.CREATED,
    )
  }

  private fun markPublished(jobItemId: UUID) {
    val updatedRows = bulkUserJobItemRepository.updateStatusIfCurrent(
      jobItemId = jobItemId,
      currentStatus = BulkUserJobItemStatus.CLAIMED,
      newStatus = BulkUserJobItemStatus.PUBLISHED,
    )
    check(updatedRows == 1) { "Bulk user job item $jobItemId could not be marked as PUBLISHED from CLAIMED" }
  }
}

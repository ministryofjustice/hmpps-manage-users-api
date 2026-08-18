package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import java.time.Duration
import java.time.Instant
import java.util.Optional

class BulkUserJobReconciliationServiceTest {
  private val bulkUserJobItemRepository: BulkUserJobItemRepository = mock()
  private val bulkUserJobRepository: BulkUserJobRepository = mock()
  private val bulkUserJobItemService: BulkUserJobItemService = mock()
  private val service = BulkUserJobReconciliationService(
    bulkUserJobItemRepository,
    bulkUserJobRepository,
    bulkUserJobItemService,
    Duration.ofHours(1),
  )

  @Test
  fun `marks job complete when no stale claimed items`() {
    val job = BulkUserJob(jiraReference = "JIRA-1", requestedBy = "userabc")
    whenever(
      bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(
        eq(job.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(emptyList())
    whenever(bulkUserJobRepository.markCompleteIfAllItemsTerminal(job.id)).thenReturn(1)

    service.reconcileBulkJob(job.id)

    verify(bulkUserJobRepository).markCompleteIfAllItemsTerminal(job.id)
    verify(bulkUserJobItemService, never()).republishStaleClaimedItem(any(), any())
  }

  @Test
  fun `republishes stale claimed items then attempts completion`() {
    val job = BulkUserJob(jiraReference = "JIRA-1", requestedBy = "userabc")
    val staleItem = BulkUserJobItem(
      username = "USER999",
      rolename = "ROLE_STALE",
      status = BulkUserJobItemStatus.CLAIMED,
      claimedAt = Instant.now().minus(Duration.ofHours(2)),
      bulkUserJob = job,
    )
    whenever(
      bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(
        eq(job.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(listOf(staleItem))
    whenever(bulkUserJobRepository.findWithJobItemsById(job.id)).thenReturn(Optional.of(job))
    whenever(bulkUserJobRepository.markCompleteIfAllItemsTerminal(job.id)).thenReturn(0)

    service.reconcileBulkJob(job.id)

    verify(bulkUserJobItemService).republishStaleClaimedItem(job, staleItem)
    verify(bulkUserJobRepository).markCompleteIfAllItemsTerminal(job.id)
  }

  @Test
  fun `continues reconciliation when a stale republish fails`() {
    val job = BulkUserJob(jiraReference = "JIRA-1", requestedBy = "userabc")
    val staleItem = BulkUserJobItem(
      username = "USER999",
      rolename = "ROLE_STALE",
      status = BulkUserJobItemStatus.CLAIMED,
      claimedAt = Instant.now().minus(Duration.ofHours(2)),
      bulkUserJob = job,
    )
    whenever(
      bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(
        eq(job.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(listOf(staleItem))
    whenever(bulkUserJobRepository.findWithJobItemsById(job.id)).thenReturn(Optional.of(job))
    whenever(bulkUserJobItemService.republishStaleClaimedItem(job, staleItem)).thenThrow(RuntimeException("publish failed"))
    whenever(bulkUserJobRepository.markCompleteIfAllItemsTerminal(job.id)).thenReturn(0)

    service.reconcileBulkJob(job.id)

    verify(bulkUserJobRepository).markCompleteIfAllItemsTerminal(job.id)
  }

  @Test
  fun `processes unprocessed created items for stale jobs then attempts completion`() {
    val job = BulkUserJob(jiraReference = "JIRA-1", requestedBy = "userabc")
    val createdItem = BulkUserJobItem(
      username = "USER111",
      rolename = "ROLE_NEW",
      status = BulkUserJobItemStatus.CREATED,
      bulkUserJob = job,
    )
    whenever(
      bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(
        eq(job.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(emptyList())
    whenever(
      bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndJobRequestedBefore(
        eq(job.id),
        eq(BulkUserJobItemStatus.CREATED),
        any(),
      ),
    ).thenReturn(listOf(createdItem))
    whenever(bulkUserJobRepository.findWithJobItemsById(job.id)).thenReturn(Optional.of(job))
    whenever(bulkUserJobRepository.markCompleteIfAllItemsTerminal(job.id)).thenReturn(0)

    service.reconcileBulkJob(job.id)

    verify(bulkUserJobItemService).processJobItem(job, createdItem)
    verify(bulkUserJobRepository).markCompleteIfAllItemsTerminal(job.id)
  }

  @Test
  fun `continues reconciliation when a created item republish fails`() {
    val job = BulkUserJob(jiraReference = "JIRA-1", requestedBy = "userabc")
    val createdItem = BulkUserJobItem(
      username = "USER111",
      rolename = "ROLE_NEW",
      status = BulkUserJobItemStatus.CREATED,
      bulkUserJob = job,
    )
    whenever(
      bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(
        eq(job.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(emptyList())
    whenever(
      bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndJobRequestedBefore(
        eq(job.id),
        eq(BulkUserJobItemStatus.CREATED),
        any(),
      ),
    ).thenReturn(listOf(createdItem))
    whenever(bulkUserJobRepository.findWithJobItemsById(job.id)).thenReturn(Optional.of(job))
    whenever(bulkUserJobItemService.processJobItem(job, createdItem)).thenThrow(RuntimeException("publish failed"))
    whenever(bulkUserJobRepository.markCompleteIfAllItemsTerminal(job.id)).thenReturn(0)

    service.reconcileBulkJob(job.id)

    verify(bulkUserJobRepository).markCompleteIfAllItemsTerminal(job.id)
  }
}

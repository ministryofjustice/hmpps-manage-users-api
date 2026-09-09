package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.event.BulkUserJobItemPublisher
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import java.time.Instant
import java.util.UUID

class BulkUserJobItemServiceTest {
  private val bulkUserJobItemRepository: BulkUserJobItemRepository = mock()
  private val bulkUserJobItemPublisher: BulkUserJobItemPublisher = mock()
  private val service = BulkUserJobItemService(bulkUserJobItemRepository, bulkUserJobItemPublisher)

  @Test
  fun `processes job item when status is created`() {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val item = BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.CREATED, bulkUserJob = job)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CREATED),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(1)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        eq(BulkUserJobItemStatus.PUBLISHED),
        any(),
      ),
    ).thenReturn(1)

    service.processJobItem(job, item)

    verify(bulkUserJobItemPublisher).publishBulkUserJobItemEvent(job, item)
    verify(bulkUserJobItemRepository).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CREATED), eq(BulkUserJobItemStatus.CLAIMED), any())
    verify(bulkUserJobItemRepository).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), any())
    assertThat(item.claimedAt).isNotNull
  }

  @Test
  fun `skips processing when item cannot be claimed`() {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val item = BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.PUBLISHED, bulkUserJob = job)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CREATED),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(0)

    service.processJobItem(job, item)

    verify(bulkUserJobItemPublisher, never()).publishBulkUserJobItemEvent(any(), any())
    verify(bulkUserJobItemRepository, never()).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), any())
  }

  @Test
  fun `leaves item published when publish fails so reconciliation can recover it`() {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val item = BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.CREATED, bulkUserJob = job)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CREATED),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(1)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        eq(BulkUserJobItemStatus.PUBLISHED),
        any(),
      ),
    ).thenReturn(1)
    whenever(bulkUserJobItemPublisher.publishBulkUserJobItemEvent(job, item)).thenThrow(RuntimeException("send failed"))

    assertThatThrownBy { service.processJobItem(job, item) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("send failed")

    verify(bulkUserJobItemRepository).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), any())
    verify(bulkUserJobItemRepository, never()).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.CREATED), any())
  }

  @Test
  fun `throws when item cannot be marked as published`() {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val item = BulkUserJobItem(id = UUID.randomUUID(), username = "USER123", rolename = "ROLE_ONE", bulkUserJob = job)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CREATED),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(1)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        eq(BulkUserJobItemStatus.PUBLISHED),
        any(),
      ),
    ).thenReturn(0)

    assertThatThrownBy { service.processJobItem(job, item) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Bulk user job item ${item.id} could not be marked as PUBLISHED from CLAIMED")
  }

  @Test
  fun `re-publishes item that is still published`() {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val item = BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.PUBLISHED, bulkUserJob = job)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.PUBLISHED),
        eq(BulkUserJobItemStatus.PUBLISHED),
        any(),
      ),
    ).thenReturn(1)

    service.republishPublishedItem(job, item)

    verify(bulkUserJobItemPublisher).publishBulkUserJobItemEvent(job, item)
  }

  @Test
  fun `skips re-publish when item is no longer published`() {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val item = BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.STARTED, bulkUserJob = job)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.PUBLISHED),
        eq(BulkUserJobItemStatus.PUBLISHED),
        any(),
      ),
    ).thenReturn(0)

    service.republishPublishedItem(job, item)

    verify(bulkUserJobItemPublisher, never()).publishBulkUserJobItemEvent(any(), any())
  }

  @Test
  fun `republishes stale claimed item and marks it published`() {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val item = BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.CLAIMED, bulkUserJob = job)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(1)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        eq(BulkUserJobItemStatus.PUBLISHED),
        any(),
      ),
    ).thenReturn(1)

    service.republishStaleClaimedItem(job, item)

    verify(bulkUserJobItemPublisher).publishBulkUserJobItemEvent(job, item)
    verify(bulkUserJobItemRepository).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), any())
  }

  @Test
  fun `skips stale republish when item is no longer claimed`() {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val item = BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.PUBLISHED, bulkUserJob = job)
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(0)

    service.republishStaleClaimedItem(job, item)

    verify(bulkUserJobItemPublisher, never()).publishBulkUserJobItemEvent(any(), any())
    verify(bulkUserJobItemRepository, never()).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), any())
  }

  @Test
  fun `leaves stale item claimed and restores original claimedAt when republish send fails`() {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val originalClaimedAt = Instant.now().minusSeconds(7200)
    val item = BulkUserJobItem(username = "USER123", rolename = "ROLE_ONE", status = BulkUserJobItemStatus.CLAIMED, bulkUserJob = job)
    item.claimedAt = originalClaimedAt
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        eq(item.id),
        eq(BulkUserJobItemStatus.CLAIMED),
        eq(BulkUserJobItemStatus.CLAIMED),
        any(),
      ),
    ).thenReturn(1)
    whenever(bulkUserJobItemPublisher.publishBulkUserJobItemEvent(job, item)).thenThrow(RuntimeException("send failed"))

    assertThatThrownBy { service.republishStaleClaimedItem(job, item) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("send failed")

    verify(bulkUserJobItemRepository).updateStatusIfCurrent(
      eq(item.id),
      eq(BulkUserJobItemStatus.CLAIMED),
      eq(BulkUserJobItemStatus.CLAIMED),
      eq(originalClaimedAt),
    )
    verify(bulkUserJobItemRepository, never()).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), any())
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.event.BulkUserJobItemPublisher
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
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
        isNull(),
      ),
    ).thenReturn(1)

    service.processJobItem(job, item)

    verify(bulkUserJobItemPublisher).publishBulkUserJobItemEvent(job, item)
    verify(bulkUserJobItemRepository).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CREATED), eq(BulkUserJobItemStatus.CLAIMED), any())
    verify(bulkUserJobItemRepository).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), isNull())
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
  fun `releases claim when publish fails`() {
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
    whenever(bulkUserJobItemPublisher.publishBulkUserJobItemEvent(job, item)).thenThrow(RuntimeException("send failed"))

    assertThatThrownBy { service.processJobItem(job, item) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("send failed")

    verify(bulkUserJobItemRepository).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.CREATED), isNull())
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
        isNull(),
      ),
    ).thenReturn(0)

    assertThatThrownBy { service.processJobItem(job, item) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Bulk user job item ${item.id} could not be marked as PUBLISHED from CLAIMED")
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
        isNull(),
      ),
    ).thenReturn(1)

    service.republishStaleClaimedItem(job, item)

    verify(bulkUserJobItemPublisher).publishBulkUserJobItemEvent(job, item)
    verify(bulkUserJobItemRepository).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), isNull())
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
    verify(bulkUserJobItemRepository, never()).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), isNull())
  }

  @Test
  fun `leaves stale item claimed when republish send fails`() {
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
    whenever(bulkUserJobItemPublisher.publishBulkUserJobItemEvent(job, item)).thenThrow(RuntimeException("send failed"))

    assertThatThrownBy { service.republishStaleClaimedItem(job, item) }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("send failed")

    verify(bulkUserJobItemRepository, never()).updateStatusIfCurrent(eq(item.id), eq(BulkUserJobItemStatus.CLAIMED), eq(BulkUserJobItemStatus.PUBLISHED), isNull())
  }
}

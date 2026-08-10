package uk.gov.justice.digital.hmpps.manageusersapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobStatus
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
class BulkUserJobItemRepositoryTest(
  @Autowired val bulkUserJobItemRepository: BulkUserJobItemRepository,
  @Autowired val bulkUserJobRepository: BulkUserJobRepository,
) {

  private val requestTime: LocalDateTime = LocalDateTime.parse("2026-08-11T10:51:02")

  @BeforeEach
  fun beforeEach() {
    bulkUserJobItemRepository.deleteAll()
    bulkUserJobRepository.deleteAll()
  }

  @Nested
  inner class FindByUserId {

    @Test
    fun `should find by Id`() {
      val job = createBulkUserJobWithStatus(BulkUserJobStatus.PENDING)
      val jobItem = createJobItemWithStatus(job, 1, BulkUserJobItemStatus.CREATED)
      job.jobItems.add(jobItem)

      bulkUserJobRepository.saveAndFlush(job)

      val actual = bulkUserJobItemRepository.findByIdOrNull(jobItem.id)
      assertThat(actual).isNotNull
      assertThat(actual!!).isEqualTo(jobItem)
    }
  }

  @Nested
  inner class StreamByBulkUserJobId {

    @Test
    fun `should return empty stream when no items exist for job id`() {
      val job = createBulkUserJobWithStatus(BulkUserJobStatus.PENDING)
      bulkUserJobRepository.saveAndFlush(job)

      bulkUserJobItemRepository.streamByBulkUserJobId(job.id).use { stream ->
        assertThat(stream).isEmpty()
      }
    }

    @Test
    fun `should return stream for expected items`() {
      val job = createBulkUserJobWithStatus(BulkUserJobStatus.COMPLETE)
      val items = listOf(
        createJobItemWithStatus(job, 1, BulkUserJobItemStatus.SUCCESS),
        createJobItemWithStatus(job, 2, BulkUserJobItemStatus.ERROR),
        createJobItemWithStatus(job, 3, BulkUserJobItemStatus.SUCCESS),
      )
      job.jobItems.addAll(items)
      bulkUserJobRepository.saveAndFlush(job)

      bulkUserJobItemRepository.streamByBulkUserJobId(job.id).use { stream ->
        assertThat(stream.toList()).containsExactlyInAnyOrderElementsOf(items)
      }
    }
  }

  @Nested
  inner class UpdateStatusIfCurrent {
    @Test
    fun `updateStatusIfCurrent sets claimedAt when claim succeeds`() {
      val job = BulkUserJob(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        status = BulkUserJobStatus.PENDING,
        jiraReference = "ABC-123",
        requestedBy = "Test",
      )
      job.addItem("user1", "role1")
      bulkUserJobRepository.save(job)

      val item = job.jobItems.first()
      val claimedAt = Instant.parse("2025-11-24T09:53:03Z")

      val updatedRows = bulkUserJobItemRepository.updateStatusIfCurrent(
        item.id,
        BulkUserJobItemStatus.CREATED,
        BulkUserJobItemStatus.CLAIMED,
        claimedAt,
      )

      assertThat(updatedRows).isEqualTo(1)
      assertThat(bulkUserJobItemRepository.findById(item.id)).isPresent.hasValueSatisfying {
        assertThat(it.status).isEqualTo(BulkUserJobItemStatus.CLAIMED)
        assertThat(it.claimedAt).isEqualTo(claimedAt)
      }
    }

    @Test
    fun `updateStatusIfCurrent clears claimedAt when release succeeds`() {
      val job = BulkUserJob(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        status = BulkUserJobStatus.PENDING,
        jiraReference = "DEF-456",
        requestedBy = "Test",
      )
      job.addItem("user1", "role1")
      bulkUserJobRepository.save(job)

      val item = job.jobItems.first()
      val claimedAt = Instant.parse("2025-11-24T09:53:03Z")

      bulkUserJobItemRepository.updateStatusIfCurrent(
        item.id,
        BulkUserJobItemStatus.CREATED,
        BulkUserJobItemStatus.CLAIMED,
        claimedAt,
      )

      val updatedRows = bulkUserJobItemRepository.updateStatusIfCurrent(
        item.id,
        BulkUserJobItemStatus.CLAIMED,
        BulkUserJobItemStatus.CREATED,
        null,
      )

      assertThat(updatedRows).isEqualTo(1)
      assertThat(bulkUserJobItemRepository.findById(item.id)).isPresent.hasValueSatisfying {
        assertThat(it.status).isEqualTo(BulkUserJobItemStatus.CREATED)
        assertThat(it.claimedAt).isNull()
      }
    }

    @Test
    fun `updateStatusIfCurrent does not update when current status does not match`() {
      val job = BulkUserJob(
        id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
        status = BulkUserJobStatus.PENDING,
        jiraReference = "GHI-789",
        requestedBy = "Test",
      )
      job.addItem("user1", "role1")
      bulkUserJobRepository.save(job)

      val item = job.jobItems.first()

      val updatedRows = bulkUserJobItemRepository.updateStatusIfCurrent(
        item.id,
        BulkUserJobItemStatus.PUBLISHED,
        BulkUserJobItemStatus.CLAIMED,
        Instant.parse("2025-11-24T09:53:03Z"),
      )

      assertThat(updatedRows).isEqualTo(0)
      assertThat(bulkUserJobItemRepository.findById(item.id)).isPresent.hasValueSatisfying {
        assertThat(it.status).isEqualTo(BulkUserJobItemStatus.CREATED)
        assertThat(it.claimedAt).isNull()
      }
    }
  }

  private fun createBulkUserJobWithStatus(status: BulkUserJobStatus) = BulkUserJob(
    id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
    status = status,
    jiraReference = "GHI-789",
    requestedBy = "Test",
    requestDateTime = requestTime.plusHours(2),
  )

  internal fun createJobItemWithStatus(job: BulkUserJob, index: Int, status: BulkUserJobItemStatus): BulkUserJobItem {
    val item = BulkUserJobItem(
      username = "user$index",
      rolename = "role$index",
      status = status,
      bulkUserJob = job,
    )
    return item
  }
}

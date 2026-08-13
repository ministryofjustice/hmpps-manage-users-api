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
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus.CLAIMED
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus.CREATED
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus.ERROR
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus.PUBLISHED
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus.STARTED
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus.SUCCESS
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobStatus
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobStatus.COMPLETE
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobStatus.PENDING
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
      val job = createBulkUserJobWithStatus(PENDING)
      val jobItem = createJobItemWithStatus(job, 1, CREATED)
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
      val job = createBulkUserJobWithStatus(PENDING)
      bulkUserJobRepository.saveAndFlush(job)

      bulkUserJobItemRepository.streamByBulkUserJobId(job.id).use { stream ->
        assertThat(stream).isEmpty()
      }
    }

    @Test
    fun `should return stream for expected items`() {
      val job = createBulkUserJobWithStatus(COMPLETE)
      val items = listOf(
        createJobItemWithStatus(job, 1, SUCCESS),
        createJobItemWithStatus(job, 2, ERROR),
        createJobItemWithStatus(job, 3, SUCCESS),
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
        status = PENDING,
        jiraReference = "ABC-123",
        requestedBy = "Test",
      )
      job.addItem("user1", "role1")
      bulkUserJobRepository.save(job)

      val item = job.jobItems.first()
      val claimedAt = Instant.parse("2025-11-24T09:53:03Z")

      val updatedRows = bulkUserJobItemRepository.updateStatusIfCurrent(
        item.id,
        CREATED,
        CLAIMED,
        claimedAt,
      )

      assertThat(updatedRows).isEqualTo(1)
      assertThat(bulkUserJobItemRepository.findById(item.id)).isPresent.hasValueSatisfying {
        assertThat(it.status).isEqualTo(CLAIMED)
        assertThat(it.claimedAt).isEqualTo(claimedAt)
      }
    }

    @Test
    fun `updateStatusIfCurrent clears claimedAt when release succeeds`() {
      val job = BulkUserJob(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        status = PENDING,
        jiraReference = "DEF-456",
        requestedBy = "Test",
      )
      job.addItem("user1", "role1")
      bulkUserJobRepository.save(job)

      val item = job.jobItems.first()
      val claimedAt = Instant.parse("2025-11-24T09:53:03Z")

      bulkUserJobItemRepository.updateStatusIfCurrent(item.id, CREATED, CLAIMED, claimedAt)

      val updatedRows = bulkUserJobItemRepository.updateStatusIfCurrent(item.id, CLAIMED, CREATED)

      assertThat(updatedRows).isEqualTo(1)
      assertThat(bulkUserJobItemRepository.findById(item.id)).isPresent.hasValueSatisfying {
        assertThat(it.status).isEqualTo(CREATED)
        assertThat(it.claimedAt).isNull()
      }
    }

    @Test
    fun `updateStatusIfCurrent does not update when current status does not match`() {
      val job = BulkUserJob(
        id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
        status = PENDING,
        jiraReference = "GHI-789",
        requestedBy = "Test",
      )
      job.addItem("user1", "role1")
      bulkUserJobRepository.save(job)

      val item = job.jobItems.first()

      val updatedRows = bulkUserJobItemRepository.updateStatusIfCurrent(item.id, PUBLISHED, CLAIMED, Instant.parse("2025-11-24T09:53:03Z"))

      assertThat(updatedRows).isEqualTo(0)
      assertThat(bulkUserJobItemRepository.findById(item.id)).isPresent.hasValueSatisfying {
        assertThat(it.status).isEqualTo(CREATED)
        assertThat(it.claimedAt).isNull()
      }
    }
  }

  @Nested
  inner class UpdateStatusAndResultIfCurrent {
    @Test
    fun `updateStatusAndResultIfCurrent sets status and result when state matches`() {
      val job = createBulkUserJobWithStatus(PENDING)
      val item = createJobItemWithStatus(job, 1, STARTED)
      job.jobItems.add(item)
      bulkUserJobRepository.saveAndFlush(job)

      val updatedRows = bulkUserJobItemRepository.updateStatusAndResultIfCurrent(item.id, STARTED, ERROR, "System issue", null)

      assertThat(updatedRows).isEqualTo(1)
      assertThat(bulkUserJobItemRepository.findById(item.id)).isPresent.hasValueSatisfying {
        assertThat(it.status).isEqualTo(ERROR)
        assertThat(it.result).isEqualTo("System issue")
      }
    }

    @Test
    fun `updateStatusAndResultIfCurrent does not update when state does not match`() {
      val job = createBulkUserJobWithStatus(PENDING)
      val item = createJobItemWithStatus(job, 1, PUBLISHED)
      job.jobItems.add(item)
      bulkUserJobRepository.saveAndFlush(job)

      val updatedRows = bulkUserJobItemRepository.updateStatusAndResultIfCurrent(item.id, STARTED, ERROR, "System issue", null)

      assertThat(updatedRows).isEqualTo(0)
      assertThat(bulkUserJobItemRepository.findById(item.id)).isPresent.hasValueSatisfying {
        assertThat(it.status).isEqualTo(PUBLISHED)
        assertThat(it.result).isNull()
      }
    }
  }

  @Nested
  inner class FindByBulkUserJobIdAndStatusAndClaimedAtBefore {
    private val cutoff: Instant = Instant.parse("2026-08-11T10:00:00Z")

    @Test
    fun `returns claimed items for the job claimed before the cutoff`() {
      val job = createBulkUserJobWithStatus(PENDING)
      val staleItem = createJobItemWithStatus(job, 1, CLAIMED).apply { claimedAt = cutoff.minusSeconds(60) }
      job.jobItems.add(staleItem)
      bulkUserJobRepository.saveAndFlush(job)

      val actual = bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(job.id, CLAIMED, cutoff)

      assertThat(actual).containsExactly(staleItem)
    }

    @Test
    fun `excludes items claimed at or after the cutoff`() {
      val job = createBulkUserJobWithStatus(PENDING)
      job.jobItems.add(createJobItemWithStatus(job, 1, CLAIMED).apply { claimedAt = cutoff })
      job.jobItems.add(createJobItemWithStatus(job, 2, CLAIMED).apply { claimedAt = cutoff.plusSeconds(60) })
      bulkUserJobRepository.saveAndFlush(job)

      val actual = bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(job.id, CLAIMED, cutoff)

      assertThat(actual).isEmpty()
    }

    @Test
    fun `excludes items with a different status`() {
      val job = createBulkUserJobWithStatus(PENDING)
      job.jobItems.add(createJobItemWithStatus(job, 1, STARTED).apply { claimedAt = cutoff.minusSeconds(60) })
      bulkUserJobRepository.saveAndFlush(job)

      val actual = bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(job.id, CLAIMED, cutoff)

      assertThat(actual).isEmpty()
    }

    @Test
    fun `excludes stale claimed items belonging to a different job`() {
      val job = BulkUserJob(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        status = PENDING,
        jiraReference = "ABC-123",
        requestedBy = "Test",
        requestDateTime = requestTime,
      )
      val otherJob = BulkUserJob(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        status = PENDING,
        jiraReference = "DEF-456",
        requestedBy = "Test",
        requestDateTime = requestTime,
      )
      job.jobItems.add(createJobItemWithStatus(job, 1, CLAIMED).apply { claimedAt = cutoff.minusSeconds(60) })
      otherJob.jobItems.add(createJobItemWithStatus(otherJob, 2, CLAIMED).apply { claimedAt = cutoff.minusSeconds(60) })
      bulkUserJobRepository.saveAndFlush(job)
      bulkUserJobRepository.saveAndFlush(otherJob)

      val actual = bulkUserJobItemRepository.findByBulkUserJobIdAndStatusAndClaimedAtBefore(job.id, CLAIMED, cutoff)

      assertThat(actual).hasSize(1)
      assertThat(actual.first().bulkUserJob.id).isEqualTo(job.id)
    }
  }

  private fun createBulkUserJobWithStatus(status: BulkUserJobStatus) = BulkUserJob(
    id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
    status = status,
    jiraReference = "GHI-789",
    requestedBy = "Test",
    requestDateTime = requestTime.plusHours(2),
  )

  private fun createJobItemWithStatus(job: BulkUserJob, index: Int, status: BulkUserJobItemStatus): BulkUserJobItem = BulkUserJobItem(
    username = "user$index",
    rolename = "role$index",
    status = status,
    bulkUserJob = job,
  )
}

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

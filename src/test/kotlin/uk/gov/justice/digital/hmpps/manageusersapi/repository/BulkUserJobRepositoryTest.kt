package uk.gov.justice.digital.hmpps.manageusersapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobStatus
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
class BulkUserJobRepositoryTest {
  @Autowired
  lateinit var bulkUserJobRepository: BulkUserJobRepository

  companion object {
    private val requestTime = LocalDateTime.parse("2025-11-24T09:53:03")

    val pendingBulkUserJob = BulkUserJob(
      id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
      status = BulkUserJobStatus.PENDING,
      jiraReference = "ABC-123",
      requestedBy = "Test",
      requestDateTime = requestTime,
    )
    val pendingBulkUserJobTwo = BulkUserJob(
      id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
      status = BulkUserJobStatus.PENDING,
      jiraReference = "DEF-456",
      requestedBy = "Second",
      requestDateTime = requestTime.plusHours(1),
    )
    val completedBulkUserJob = BulkUserJob(
      id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
      status = BulkUserJobStatus.COMPLETE,
      jiraReference = "GHI-789",
      requestedBy = "Test",
      requestDateTime = requestTime.plusHours(2),
    )
  }

  @BeforeEach
  fun setup() {
    bulkUserJobRepository.deleteAll()
  }

  fun givenBulkJobsExist() {
    bulkUserJobRepository.save(pendingBulkUserJob)
    bulkUserJobRepository.save(pendingBulkUserJobTwo)
    bulkUserJobRepository.save(completedBulkUserJob)
  }
   
  @Test
  fun `persists bulk user job entity`() {
    val bulkUserJob = BulkUserJob(jiraReference = "JIRA-111", requestedBy = "user1")
    bulkUserJob.addItem("user-432", "role_test_one")
    bulkUserJob.addItem("user-765", "role_test_two")
    bulkUserJob.addItem("user-987", "role_test_three")
    bulkUserJobRepository.save(bulkUserJob)

    val result = bulkUserJobRepository.findById(bulkUserJob.id)

    assertThat(result).isPresent.hasValueSatisfying {
      assertThat(it).usingRecursiveComparison().ignoringFields("jobItems").isEqualTo(bulkUserJob)
      assertThat(it.jobItems).usingRecursiveFieldByFieldElementComparatorIgnoringFields("job")
        .containsExactlyInAnyOrderElementsOf(bulkUserJob.jobItems)
    }
  }

  @Nested
  inner class FindByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase {
    @Test
    fun `findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase returns only bulk jobs where the given string is contained within the jira reference`() {
      givenBulkJobsExist()

      val result =
        bulkUserJobRepository.findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase(
          "ABC",
          "ABC",
          Pageable.unpaged(Sort.by("RequestDateTime").descending()),
        ).content

      assertThat(bulkUserJobRepository.findAll()).hasSize(3)
      assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(pendingBulkUserJob)
    }

    @Test
    fun `findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase returns only bulk jobs where the given string is contained within requested by`() {
      givenBulkJobsExist()

      val result =
        bulkUserJobRepository.findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase(
          "Test",
          "Test",
          Pageable.unpaged(Sort.by("RequestDateTime").descending()),
        ).content

      assertThat(bulkUserJobRepository.findAll()).hasSize(3)
      assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(completedBulkUserJob, pendingBulkUserJob)
    }

    @Test
    fun `findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase returns only bulk jobs for page when specified`() {
      givenBulkJobsExist()

      val result =
        bulkUserJobRepository.findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase(
          "",
          "",
          PageRequest.of(2, 1, Sort.by("RequestDateTime").descending()),
        ).content

      assertThat(bulkUserJobRepository.findAll()).hasSize(3)
      assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(pendingBulkUserJobTwo)
    }

    @Test
    fun `findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase returns only bulk jobs for page when specified snd search provided`() {
      givenBulkJobsExist()

      val result =
        bulkUserJobRepository.findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase(
          "Test",
          "Test",
          PageRequest.of(1, 1, Sort.by("RequestDateTime").descending()),
        ).content

      assertThat(bulkUserJobRepository.findAll()).hasSize(3)
      assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(pendingBulkUserJob)
    }
    
    @Test
    fun `findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase returns all bulk jobs when no search or pagination`() {
      givenBulkJobsExist()

      val result =
        bulkUserJobRepository.findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase(
          "",
          "",
          Pageable.unpaged(
            Sort.by("RequestDateTime").descending(),
          ),
        ).content

      assertThat(bulkUserJobRepository.findAll()).hasSize(3)
      assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(completedBulkUserJob, pendingBulkUserJobTwo, pendingBulkUserJob)
    }

    @Test
    fun `findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase search is case insensitive`() {
      givenBulkJobsExist()

      val result =
        bulkUserJobRepository.findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase(
          "TEST",
          "TEST",
          Pageable.unpaged(Sort.by("RequestDateTime").descending()),
        ).content

      assertThat(bulkUserJobRepository.findAll()).hasSize(3)
      assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(completedBulkUserJob, pendingBulkUserJob)
    }
  }
}

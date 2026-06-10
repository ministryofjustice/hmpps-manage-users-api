package uk.gov.justice.digital.hmpps.manageusersapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob

@DataJpaTest
class BulkUserJobRepositoryTest {
  @Autowired
  lateinit var bulkUserJobRepository: BulkUserJobRepository

  @BeforeEach
  fun setup() {
    bulkUserJobRepository.deleteAll()
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
}

package uk.gov.justice.digital.hmpps.manageusersapi.scheduled

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob.BulkUserJobReconciliationService
import java.util.UUID

class BulkUserJobReconcilerTest {
  private val bulkUserJobRepository: BulkUserJobRepository = mock()
  private val bulkUserJobReconciliationService: BulkUserJobReconciliationService = mock()
  private val scheduler = BulkUserJobReconciler(bulkUserJobRepository, bulkUserJobReconciliationService)

  @Test
  fun `reconciles every incomplete job`() {
    val jobOne = UUID.randomUUID()
    val jobTwo = UUID.randomUUID()
    whenever(bulkUserJobRepository.findIncompleteJobIds()).thenReturn(listOf(jobOne, jobTwo))

    scheduler.reconcileIncompleteJobs()

    verify(bulkUserJobReconciliationService).reconcileBulkJob(jobOne)
    verify(bulkUserJobReconciliationService).reconcileBulkJob(jobTwo)
  }

  @Test
  fun `does nothing when there are no incomplete jobs`() {
    whenever(bulkUserJobRepository.findIncompleteJobIds()).thenReturn(emptyList())

    scheduler.reconcileIncompleteJobs()

    verify(bulkUserJobReconciliationService, never()).reconcileBulkJob(org.mockito.kotlin.any())
  }

  @Test
  fun `continues reconciling remaining jobs when one fails`() {
    val jobOne = UUID.randomUUID()
    val jobTwo = UUID.randomUUID()
    whenever(bulkUserJobRepository.findIncompleteJobIds()).thenReturn(listOf(jobOne, jobTwo))
    whenever(bulkUserJobReconciliationService.reconcileBulkJob(jobOne)).thenThrow(RuntimeException("boom"))

    scheduler.reconcileIncompleteJobs()

    verify(bulkUserJobReconciliationService).reconcileBulkJob(jobTwo)
  }
}

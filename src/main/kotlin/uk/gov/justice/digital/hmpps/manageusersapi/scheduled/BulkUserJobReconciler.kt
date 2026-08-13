package uk.gov.justice.digital.hmpps.manageusersapi.scheduled

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob.BulkUserJobReconciliationService

@Component
class BulkUserJobReconciler(
  private val bulkUserJobRepository: BulkUserJobRepository,
  private val bulkUserJobReconciliationService: BulkUserJobReconciliationService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Scheduled(
    initialDelayString = "\${application.bulk-jobs.reconciliation-interval}",
    fixedDelayString = "\${application.bulk-jobs.reconciliation-interval}",
  )
  fun reconcileIncompleteJobs() {
    val incompleteJobIds = bulkUserJobRepository.findIncompleteJobIds()
    if (incompleteJobIds.isEmpty()) return

    log.info("Reconciling {} incomplete bulk user job(s)", incompleteJobIds.size)
    incompleteJobIds.forEach { jobId ->
      try {
        bulkUserJobReconciliationService.reconcileBulkJob(jobId)
      } catch (e: Exception) {
        log.error("Failed to reconcile bulk user job {}", jobId, e)
      }
    }
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.event

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob.BulkUserJobItemRoleAssignmentService

@Service
class BulkUserJobItemListener(
  private val bulkUserJobItemRoleAssignmentService: BulkUserJobItemRoleAssignmentService,
) {
  @SqsListener(
    value = ["bulkuserjobitemqueue"],
    factory = "hmppsQueueContainerFactoryProxy",
    maxConcurrentMessages = "\${application.bulk-jobs.throttling.max-concurrent-messages}",
    maxMessagesPerPoll = "\${application.bulk-jobs.throttling.max-messages-per-poll}",
  )
  fun onBulkUserJobItemMessage(message: BulkUserJobItemMessage) {
    bulkUserJobItemRoleAssignmentService.processRoleAssignmentMessage(message)
  }
}

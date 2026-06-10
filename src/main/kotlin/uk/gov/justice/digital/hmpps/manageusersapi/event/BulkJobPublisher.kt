package uk.gov.justice.digital.hmpps.manageusersapi.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.UUID

@Service
class BulkJobPublisher(
  hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {
  private val bulkUserJobQueue by lazy {
    hmppsQueueService.findByQueueId("bulkuserjobqueue") ?: throw QueueNotFoundException("bulkuserjobqueue")
  }
  private val bulkUserJobQueueUrl by lazy { bulkUserJobQueue.queueUrl }
  private val bulkUserJobSqsClient by lazy { bulkUserJobQueue.sqsClient }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun publishBulkUserJobEvent(job: BulkUserJob) {
    val bulkUserJobEvent = BulkUserJobEvent(job.id)
    bulkUserJobSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(bulkUserJobQueueUrl)
        .messageBody(objectMapper.writeValueAsString(bulkUserJobEvent))
        .build(),
    )
    log.info("Published bulk user job event: ${job.id}")
  }
}

@Schema(description = "Bulk User Job Event")
data class BulkUserJobEvent(
  @Schema(description = "Id of the bulk job", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
  val jobId: UUID,
)

class QueueNotFoundException(queueId: String) : RuntimeException("Queue with id $queueId does not exist")

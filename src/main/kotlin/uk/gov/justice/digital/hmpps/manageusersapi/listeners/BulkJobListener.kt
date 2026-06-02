package uk.gov.justice.digital.hmpps.manageusersapi.listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.RateLimiter
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.manageusersapi.listeners.model.BulkJobEvent
import uk.gov.justice.digital.hmpps.manageusersapi.listeners.model.BulkJobItemEvent
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkJobItemStatus
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import kotlin.jvm.optionals.getOrElse

@Service
class BulkJobListener(
  private val objectMapper: ObjectMapper,
  private val bulkJobRepository: BulkJobRepository,
  private val bulkJobItemRepository: BulkJobItemRepository,
  private val hmppsQueueService: HmppsQueueService,
  @Qualifier("bulkJobItemRateLimiter") private val bulkJobItemRateLimiter: RateLimiter,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
  
  @SqsListener("bulkjobqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onBulkJobEvent(message: String) {
    log.info("Received bulk job event")
    val bulkJobEvent: BulkJobEvent = objectMapper.readValue(message, BulkJobEvent::class.java)
    
    bulkJobRepository.findById(bulkJobEvent.id).ifPresentOrElse({
      bulkJobItemRepository.findAllByBulkJobIdAndStatusIs(it.id, BulkJobItemStatus.Created).forEach { bulkJobItem ->  
        val bulkJobItemQueue = hmppsQueueService.findByQueueId("bulkjobitemqueue")
        bulkJobItemQueue?.sqsClient?.sendMessage(
          SendMessageRequest.builder()
            .queueUrl(bulkJobItemQueue.queueUrl)
            .messageBody(objectMapper.writeValueAsString(BulkJobItemEvent(id = bulkJobItem.id)))
            .build(),
        )
        bulkJobItemRepository.save(bulkJobItem.pending())
      }
    }, {
      throw RuntimeException("Bulk job not found for ID ${bulkJobEvent.id}")
    })
    log.info("Processed bulk job event: ${bulkJobEvent.id}")
  }

  @SqsListener("bulkjobitemqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun onBulkJobItemEvent(message: String) {
    bulkJobItemRateLimiter.acquire()
    log.info("Received bulk job item event")
    val bulkJobItemEvent: BulkJobItemEvent = objectMapper.readValue(message, BulkJobItemEvent::class.java)

    val bulkItem = bulkJobItemRepository.findById(bulkJobItemEvent.id).getOrElse {
      throw RuntimeException("Bulk job item not found for ID ${bulkJobItemEvent.id}")
    }
    // pretend to call nomis-user-roles-api here
    Thread.sleep(1000)
    // update item to completed
    bulkJobItemRepository.save(bulkItem.complete())
    
    log.info("Processed bulk job item event: ${bulkItem.id} - role ${bulkItem.rolename} user ${bulkItem.username}")
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobStatus
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.UUID
import java.util.concurrent.CompletableFuture

class BulkJobPublisherTest {

  private val hmppsQueueService: HmppsQueueService = mock()
  private val sqsClient: SqsAsyncClient = mock()
  private val publisher = BulkJobPublisher(hmppsQueueService, ObjectMapper())

  @Test
  fun `should publish bulk user job event`() {
    val bulkJob = BulkUserJob(UUID.randomUUID(), "JIRA-123", BulkUserJobStatus.PENDING, "userabc")
    val bulkUserJobQueue = HmppsQueue(UUID.randomUUID().toString(), sqsClient, "bulkuserjobqueue")
    whenever(hmppsQueueService.findByQueueId("bulkuserjobqueue")).thenReturn(bulkUserJobQueue)
    whenever(sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName("bulkuserjobqueue").build())).thenReturn(
      CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl("sqs://bulkuserjobqueue").build()),
    )

    publisher.publishBulkUserJobEvent(bulkJob)

    verify(sqsClient).sendMessage(
      SendMessageRequest.builder().queueUrl("sqs://bulkuserjobqueue").messageBody("{\"jobId\":\"${bulkJob.id}\"}")
        .build(),
    )
  }

  @Test
  fun `should throw queue not found exception when not exists`() {
    val bulkJob = BulkUserJob(UUID.randomUUID(), "JIRA-123", BulkUserJobStatus.PENDING, "userabc")

    assertThatThrownBy { publisher.publishBulkUserJobEvent(bulkJob) }
      .isInstanceOf(QueueNotFoundException::class.java)
      .hasMessage("Queue with id bulkuserjobqueue does not exist")
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.justice.hmpps.sqs.HmppsQueueFactory
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException

@TestConfiguration
class SqsConfig(private val hmppsQueueFactory: HmppsQueueFactory) {
  @Bean("bulkuserjobqueue-sqs-client")
  fun bulkUserJobQueueSqsClient(
    hmppsSqsProperties: HmppsSqsProperties,
    @Qualifier("bulkuserjobqueue-sqs-dlq-client") bulkUserJobQueueSqsDlqClient: SqsAsyncClient,
    @Value("\${hmpps.sqs.queues.bulkuserjobqueue.queueName}") bulkUserJobQueueName: String,
  ): SqsAsyncClient = with(hmppsSqsProperties) {
    val config = queues["bulkuserjobqueue"]
      ?: throw MissingQueueException("HmppsSqsProperties config for bulkuserjobqueue not found")
    hmppsQueueFactory.createSqsAsyncClient(config, hmppsSqsProperties, bulkUserJobQueueSqsDlqClient)
  }
}

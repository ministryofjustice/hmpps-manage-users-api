package uk.gov.justice.digital.hmpps.manageusersapi.config

import com.google.common.util.concurrent.RateLimiter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SqsConfig {
  
  @Bean
  fun bulkJobItemRateLimiter(@Value("\${hmpps.sqs.queues.bulkjobitemqueue.rateLimit}") rateLimit: Double): RateLimiter {
    return RateLimiter.create(rateLimit)
  }
}
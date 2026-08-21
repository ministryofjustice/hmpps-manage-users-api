package uk.gov.justice.digital.hmpps.manageusersapi.config

import com.google.common.util.concurrent.RateLimiter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BulkJobConfiguration {

  /**
   * Rate limiter that throttles bulk role-assignment calls to the nomis user roles api.
   */
  @Bean
  fun rolesApiRateLimiter(
    @Value("\${application.bulk-jobs.throttling.api-permits-per-second}") permitsPerSecond: Double,
  ): RateLimiter = RateLimiter.create(permitsPerSecond)
}

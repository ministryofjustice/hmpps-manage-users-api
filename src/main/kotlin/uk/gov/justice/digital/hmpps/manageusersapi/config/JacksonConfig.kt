package uk.gov.justice.digital.hmpps.manageusersapi.config

import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

  @Bean
  fun kotlinModule(): KotlinModule = KotlinModule.Builder()
    .configure(KotlinFeature.NullIsSameAsDefault, true)
    .configure(KotlinFeature.NullToEmptyMap, true)
    .build()
}

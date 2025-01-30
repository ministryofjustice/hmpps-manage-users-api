package uk.gov.justice.digital.hmpps.manageusersapi.config

import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils

@Configuration
class NomisWebClientConfiguration(appContext: ApplicationContext) : AbstractWebClientConfiguration(appContext, "nomis") {

  private val environment = appContext.environment

  private val maxRetryAttempts = environment.getRequiredProperty("nomis.max-retry-attempts", Long::class.java)
  private val extendedTimeoutMaxRetryAttempts =
    environment.getRequiredProperty("nomis.extended-timeout-max-retry-attempts", Long::class.java)

  @Bean("nomisClientRegistration")
  fun getNomisClientRegistration(): ClientRegistration = getClientRegistration()

  @Bean
  fun nomisWebClient(builder: Builder, authorizedClientManager: OAuth2AuthorizedClientManager) = getWebClient(builder, authorizedClientManager)

  @Bean
  fun nomisUserWebClient(builder: Builder) = getWebClientWithCurrentUserToken(builder)

  @Bean
  fun nomisUserExtendedTimeoutWebClient(builder: Builder) = getExtendedTimeoutWebClientWithCurrentUserToken(builder)

  @Bean
  fun nomisHealthWebClient(builder: Builder): WebClient = getHealthWebClient(builder)

  @Bean
  fun nomisWebClientUtils(nomisWebClient: WebClient) = WebClientUtils(nomisWebClient, maxRetryAttempts)

  @Bean
  fun nomisUserWebClientUtils(nomisUserWebClient: WebClient) = WebClientUtils(nomisUserWebClient, maxRetryAttempts)

  @Bean
  fun nomisUserExtendedTimeoutWebClientUtils(nomisUserExtendedTimeoutWebClient: WebClient) = WebClientUtils(nomisUserExtendedTimeoutWebClient, extendedTimeoutMaxRetryAttempts)
}

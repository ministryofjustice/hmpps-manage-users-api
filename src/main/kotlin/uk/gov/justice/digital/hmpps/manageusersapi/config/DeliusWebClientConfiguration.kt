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
class DeliusWebClientConfiguration(appContext: ApplicationContext) :
  AbstractWebClientConfiguration(appContext, "delius") {

  @Bean("deliusClientRegistration")
  fun getDeliusClientRegistration(): ClientRegistration = getClientRegistration()

  @Bean
  fun deliusWebClient(builder: Builder, authorizedClientManager: OAuth2AuthorizedClientManager): WebClient =
    getWebClient(builder, authorizedClientManager, "/secure")

  @Bean
  fun deliusHealthWebClient(builder: Builder): WebClient = getHealthWebClient(builder)

  @Bean
  fun deliusWebClientUtils(deliusWebClient: WebClient) = WebClientUtils(deliusWebClient)
}
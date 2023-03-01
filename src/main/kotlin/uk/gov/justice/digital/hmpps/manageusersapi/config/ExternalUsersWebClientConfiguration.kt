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
class ExternalUsersWebClientConfiguration(appContext: ApplicationContext) : AbstractWebClientConfiguration(appContext, "external-users") {

  @Bean("externalUsersClientRegistration")
  fun getExternalUsersClientRegistration(): ClientRegistration = getClientRegistration()

  @Bean
  fun externalUsersWebClient(builder: Builder, authorizedClientManager: OAuth2AuthorizedClientManager) =
    getWebClient(builder, authorizedClientManager)

  @Bean
  fun externalUsersUserWebClient(builder: Builder) = getWebClientWithCurrentUserToken(builder)

  @Bean
  fun externalUsersHealthWebClient(builder: Builder): WebClient = getHealthWebClient(builder)

  @Bean
  fun externalUsersWebClientUtils(externalUsersWebClient: WebClient) = WebClientUtils(externalUsersWebClient)

  @Bean
  fun externalUsersUserWebClientUtils(externalUsersUserWebClient: WebClient) = WebClientUtils(externalUsersUserWebClient)
}

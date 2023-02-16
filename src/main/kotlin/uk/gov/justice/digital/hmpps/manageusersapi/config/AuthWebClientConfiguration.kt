package uk.gov.justice.digital.hmpps.manageusersapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils

@Configuration
class AuthWebClientConfiguration(
  @Value("\${api.base.url.oauth}") val authBaseUri: String,
  appContext: ApplicationContext
) :
  AbstractWebClientConfiguration(appContext, "hmpps-auth") {

  @Bean("authClientRegistration")
  fun getAuthClientRegistration(): ClientRegistration = getClientRegistration()

  @Bean
  fun authWebClient(builder: Builder, authorizedClientManager: OAuth2AuthorizedClientManager): WebClient =
    getWebClient(builder, authorizedClientManager)

  @Bean
  fun authHealthWebClient(builder: Builder): WebClient = builder.baseUrl(authBaseUri).build()

  @Bean
  fun authWebClientUtils(authWebClient: WebClient) = WebClientUtils(authWebClient)
}

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
class AuthWebClientConfiguration(appContext: ApplicationContext) : AbstractWebClientConfiguration(appContext, "hmpps-auth") {

  private val environment = appContext.environment

  private val maxRetryAttempts = environment.getRequiredProperty("hmpps-auth.max-retry-attempts", Long::class.java)

  @Bean("authClientRegistration")
  fun getAuthClientRegistration(): ClientRegistration = getClientRegistration()

  @Bean
  fun authWebClient(builder: Builder, authorizedClientManager: OAuth2AuthorizedClientManager): WebClient =
    getWebClient(builder, authorizedClientManager)

  @Bean
  fun authUserWebClient(builder: Builder) =
    getWebClientWithCurrentUserToken(builder)

  @Bean
  fun authHealthWebClient(builder: Builder): WebClient = getHealthWebClient(builder)

  @Bean
  fun authWebClientUtils(authWebClient: WebClient) = WebClientUtils(authWebClient, maxRetryAttempts)

  @Bean
  fun authUserWebClientUtils(authUserWebClient: WebClient) = WebClientUtils(authUserWebClient, maxRetryAttempts)
}

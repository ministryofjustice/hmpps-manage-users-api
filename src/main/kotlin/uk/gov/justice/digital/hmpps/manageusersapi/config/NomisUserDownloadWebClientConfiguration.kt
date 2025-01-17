package uk.gov.justice.digital.hmpps.manageusersapi.config

import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils

@Configuration
class NomisUserDownloadWebClientConfiguration(appContext: ApplicationContext) :
  AbstractWebClientConfiguration(appContext, "nomis-user-download") {

  private val environment = appContext.environment

  private val maxRetryAttempts = environment.getRequiredProperty("nomis-user-download.max-retry-attempts", Long::class.java)

  @Bean("nomisUserDownloadClientRegistration")
  fun getNomisUserDownloadClientRegistration(): ClientRegistration = getClientRegistration()

  @Bean
  fun nomisUserDownloadWebClient(builder: Builder) = getWebClientWithCurrentUserToken(builder)

  @Bean
  fun nomisUserDownloadWebClientUtils(nomisUserDownloadWebClient: WebClient) = WebClientUtils(nomisUserDownloadWebClient, maxRetryAttempts)
}

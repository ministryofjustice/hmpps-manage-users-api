package uk.gov.justice.digital.hmpps.manageusersapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.utils.UserContext

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.oauth}") val authBaseUri: String,
  @Value("\${api.base.url.nomis}") val nomisBaseUri: String,
  @Value("\${api.base.url.external}") val externalUsersBaseUri: String,
  @Value("\${api.base.url.delius}") val deliusBaseUri: String,
  appContext: ApplicationContext
) :
  AbstractWebClientConfiguration(
    appContext, "hmpps-auth"
  ) {

  @Bean
  fun authWebClient(builder: WebClient.Builder): WebClient {
    return builder
      .baseUrl(authBaseUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  @Bean("authWebWithClientId")
  fun authWebWithClientId(
    builder: WebClient.Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager
  ): WebClient = getWebClient(builder, authorizedClientManager)

  @Bean
  fun externalUsersWebClient(builder: WebClient.Builder): WebClient {
    return builder
      .baseUrl(externalUsersBaseUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  @Bean
  fun nomisWebClient(builder: WebClient.Builder): WebClient {
    return builder
      .baseUrl(nomisBaseUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  @Bean
  fun deliusWebClient(builder: WebClient.Builder) =
    builder
      .baseUrl(deliusBaseUri)
      .filter(addAuthHeaderFilterFunction())
      .build()

  @Bean
  fun externalUsersWebClientUtils(externalUsersWebClient: WebClient) =
    WebClientUtils(externalUsersWebClient)

  @Bean
  fun nomisWebClientUtils(nomisWebClient: WebClient) =
    WebClientUtils(nomisWebClient)

  @Bean
  fun authWebClientUtils(authWebClient: WebClient) =
    WebClientUtils(authWebClient)

  @Bean
  fun deliusWebClientUtils(deliusWebClient: WebClient) =
    WebClientUtils(deliusWebClient)

  @Bean("authClientRegistration")
  fun getAuthClientRegistration(): ClientRegistration = getClientRegistration()

  private fun addAuthHeaderFilterFunction(): ExchangeFilterFunction {
    return ExchangeFilterFunction { request: ClientRequest?, next: ExchangeFunction ->
      val filtered = ClientRequest.from(request)
        .header(HttpHeaders.AUTHORIZATION, UserContext.getAuthToken())
        .build()
      next.exchange(filtered)
    }
  }
}

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
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.utils.UserContext

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.oauth}") val authBaseUri: String,
  @Value("\${api.base.url.external}") val externalUsersBaseUri: String,
  appContext: ApplicationContext
) :
  AbstractWebClientConfiguration(
    appContext, "hmpps-auth"
  ) {

  @Bean
  fun authWebClient(
    builder: Builder,
    authorizedClientManager: OAuth2AuthorizedClientManager
  ): WebClient = getWebClient(builder, authorizedClientManager)

  @Bean
  fun externalUsersWebClient(builder: Builder): WebClient {
    return builder
      .baseUrl(externalUsersBaseUri)
      .filter(addAuthHeaderFilterFunction())
      .build()
  }

  @Bean
  fun authHealthWebClient(builder: Builder): WebClient = builder.baseUrl(authBaseUri).build()

  @Bean
  fun externalUsersHealthWebClient(builder: Builder): WebClient = builder.baseUrl(externalUsersBaseUri).build()

  @Bean
  fun authWebClientUtils(authWebClient: WebClient) =
    WebClientUtils(authWebClient)

  @Bean
  fun externalUsersWebClientUtils(externalUsersWebClient: WebClient) =
    WebClientUtils(externalUsersWebClient)

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

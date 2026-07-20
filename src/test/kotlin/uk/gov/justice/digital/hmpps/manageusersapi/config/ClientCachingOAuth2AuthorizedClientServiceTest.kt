package uk.gov.justice.digital.hmpps.manageusersapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import java.time.Instant

class ClientCachingOAuth2AuthorizedClientServiceTest {
  private val clientRegistrationRepository: ClientRegistrationRepository = mock()
  private val service = ClientCachingOAuth2AuthorizedClientService(clientRegistrationRepository)

  @Test
  fun `loadAuthorizedClient returns null when client has not been cached`() {
    whenever(clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID)).thenReturn(clientRegistration())

    val loadedClient = service.loadAuthorizedClient<OAuth2AuthorizedClient>(REGISTRATION_ID, "any-principal")

    assertThat(loadedClient).isNull()
  }

  @Test
  fun `loadAuthorizedClient returns cached client regardless of principal name`() {
    val registration = clientRegistration()
    whenever(clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID)).thenReturn(registration)
    val cachedClient = OAuth2AuthorizedClient(registration, "principalName", accessToken())
    service.saveAuthorizedClient(cachedClient, TestingAuthenticationToken("different-principal", "credentials"))

    val loadedClient = service.loadAuthorizedClient<OAuth2AuthorizedClient>(REGISTRATION_ID, "another-principal")

    assertThat(loadedClient).isEqualTo(cachedClient)
  }

  private fun clientRegistration() = ClientRegistration.withRegistrationId(REGISTRATION_ID)
    .clientId("client-id")
    .clientSecret("secret")
    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
    .tokenUri("https://example.test/oauth/token")
    .build()

  private fun accessToken() = Instant.now().let { now ->
    OAuth2AccessToken(
      OAuth2AccessToken.TokenType.BEARER,
      "access-token",
      now,
      now.plusSeconds(300),
    )
  }
  companion object {
    private const val REGISTRATION_ID = "delius"
  }
}

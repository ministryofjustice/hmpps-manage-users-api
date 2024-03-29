package uk.gov.justice.digital.hmpps.manageusersapi.config

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientId
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import java.util.concurrent.ConcurrentHashMap

// Ignore the passed in principal and use the same value each time
private const val SINGLE_PRINCIPAL = "principalName"

class ClientCachingOAuth2AuthorizedClientService(private val clientRegistrationRepository: ClientRegistrationRepository) : OAuth2AuthorizedClientService {
  private val authorizedClients: MutableMap<OAuth2AuthorizedClientId, OAuth2AuthorizedClient> = ConcurrentHashMap()

  override fun <T : OAuth2AuthorizedClient?> loadAuthorizedClient(
    clientRegistrationId: String,
    principalName: String,
  ): T? = clientRegistrationRepository.findByRegistrationId(clientRegistrationId)?.let {
    @Suppress("UNCHECKED_CAST")
    authorizedClients[OAuth2AuthorizedClientId(clientRegistrationId, SINGLE_PRINCIPAL)] as T
  }

  override fun saveAuthorizedClient(authorizedClient: OAuth2AuthorizedClient, principal: Authentication) {
    authorizedClients[
      OAuth2AuthorizedClientId(authorizedClient.clientRegistration.registrationId, SINGLE_PRINCIPAL),
    ] = authorizedClient
  }

  override fun removeAuthorizedClient(clientRegistrationId: String, principalName: String) {
    clientRegistrationRepository.findByRegistrationId(clientRegistrationId)?.apply {
      authorizedClients.remove(OAuth2AuthorizedClientId(clientRegistrationId, SINGLE_PRINCIPAL))
    }
  }
}

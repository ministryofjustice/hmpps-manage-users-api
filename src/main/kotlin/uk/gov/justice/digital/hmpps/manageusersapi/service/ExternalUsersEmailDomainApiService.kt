package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class ExternalUsersEmailDomainApiService(
  @Qualifier("externalUsersWebClient") val externalUsersWebClient: WebClient
) {



}

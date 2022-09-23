package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role

@Service
class ExternalUsersApiService(@Qualifier("externalUsersWebClient") val externalUsersWebClient: WebClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getRoles(adminTypes: List<AdminType>?): List<Role> {
    return externalUsersWebClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/api/roles")
          .queryParam("adminTypes", adminTypes)
          .build()
      }
      .retrieve()
      .bodyToMono(RoleList::class.java)
      .block()!!
  }
}

class RoleList : MutableList<Role> by ArrayList()

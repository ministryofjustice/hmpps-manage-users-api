package uk.gov.justice.digital.hmpps.manageusersapi.adapter

import org.springframework.web.reactive.function.client.WebClient

class WebClientUtils(private val client: WebClient) {

  fun <T : Any> get(uri: String, elementClass: Class<T>): T =
    client.get()
      .uri(uri)
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!

  fun <T : Any> getIfPresent(uri: String, elementClass: Class<T>): T? =
    client.get()
      .uri(uri)
      .retrieve()
      .bodyToMono(elementClass)
      .block()
}

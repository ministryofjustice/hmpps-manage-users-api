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

  fun put(uri: String, body: Any) {
    client.put()
      .uri(uri)
      .bodyValue(body)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun put(uri: String) {
    client.put()
      .uri(uri)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun <T : Any> putWithResponse(uri: String, elementClass: Class<T>): T =
    client.put()
      .uri(uri)
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!
}

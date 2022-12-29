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

  fun <T : Any> getWithParams(uri: String, elementClass: Class<T>, queryParams: Map<String, Any?>): T =
    client.get()
      .uri { uriBuilder ->
        uriBuilder.path(uri)
        queryParams.forEach { (key, value) ->
          value?.let {
            if(value is Collection<*>) {
              uriBuilder.queryParam(key, value)
            } else {
              uriBuilder.queryParam(key, value)
            }
          }?: run { uriBuilder.queryParam(key, value) }
        }
        uriBuilder.build()
      }
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!

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

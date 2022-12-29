package uk.gov.justice.digital.hmpps.manageusersapi.adapter

import java.net.URI
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder

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
      .uri { uriBuilder -> buildURI(uri, queryParams, uriBuilder) }
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!

  fun <T : Any> getWithParams(uri: String, elementClass: ParameterizedTypeReference<T>, queryParams: Map<String, Any?>): T =
    client.get()
      .uri { uriBuilder -> buildURI(uri, queryParams, uriBuilder) }
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!

  private fun buildURI(path: String, queryParams: Map<String, Any?>, uriBuilder: UriBuilder): URI {
    uriBuilder.path(path)
    queryParams.forEach { (key, value) ->
      value?.let {
        if (value is Collection<*>) {
          uriBuilder.queryParam(key, value)
        } else {
          uriBuilder.queryParam(key, value)
        }
      } ?: run { uriBuilder.queryParam(key, value) }
    }
    return uriBuilder.build()
  }


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

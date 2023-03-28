package uk.gov.justice.digital.hmpps.manageusersapi.adapter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import java.net.URI

class WebClientUtils(private val client: WebClient) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getWithEmptyResponseSucceeds(uri: String): Boolean {
    client.get()
      .uri(uri)
      .retrieve()
      .toBodilessEntity()
      .block()
    return true
  }

  fun <T : Any> get(uri: String, elementClass: Class<T>): T =
    client.get()
      .uri(uri)
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!

  fun <T : Any> getIgnoreError(uri: String, elementClass: Class<T>): T? {
    return client.get()
      .uri(uri)
      .retrieve()
      .bodyToMono(elementClass)
      .onErrorResume {
        log.warn("Unable to retrieve details due to {}", it.message)
        Mono.empty()
      }
      .block()
  }

  fun <T : Any> getIfPresent(uri: String, elementClass: Class<T>): T? =
    client.get()
      .uri(uri)
      .retrieve()
      .bodyToMono(elementClass)
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .block()

  fun <T : Any> getWithParams(uri: String, elementClass: Class<T>, queryParams: Map<String, Any?>): T =
    client.get()
      .uri { uriBuilder -> uriBuilder.buildURI(uri, queryParams) }
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!

  fun <T : Any> getWithParams(uri: String, elementClass: ParameterizedTypeReference<T>, queryParams: Map<String, Any?>): T =
    client.get()
      .uri { uriBuilder -> uriBuilder.buildURI(uri, queryParams) }
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

  fun post(uri: String, body: Any) {
    client.post()
      .uri(uri)
      .bodyValue(body)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun <T : Any> postWithResponse(uri: String, body: Any, elementClass: Class<T>): T =
    client.post()
      .uri(uri)
      .bodyValue(body)
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!

  fun <T : Any> postWithResponse(uri: String, elementClass: Class<T>): T =
    client.post()
      .uri(uri)
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!

  fun delete(uri: String) {
    client.delete()
      .uri(uri)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  private fun UriBuilder.buildURI(path: String, queryParams: Map<String, Any?>): URI {
    path(path)
    queryParams.forEach { (key, value) ->
      value?.let {
        // Force usage of correct overloaded queryParam method
        if (value is Collection<*>) {
          queryParam(key, value)
        } else {
          queryParam(key, "{$key}")
        }
      } ?: run { queryParam(key, value) }
    }
    return build(queryParams)
  }
}

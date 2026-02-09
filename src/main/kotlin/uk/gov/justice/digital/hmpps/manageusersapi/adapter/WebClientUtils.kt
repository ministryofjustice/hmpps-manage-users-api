package uk.gov.justice.digital.hmpps.manageusersapi.adapter

import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import reactor.util.retry.Retry.max
import java.net.URI

class WebClientUtils(
  private val client: WebClient,
  private val maxRetryAttempts: Long,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getWithEmptyResponseSucceeds(uri: String, vararg uriVariables: Any?): Boolean {
    client.get()
      .uri(uri, *uriVariables)
      .retrieve()
      .toBodilessEntity()
      .withRetryPolicy()
      .block()
    return true
  }

  fun <T : Any> get(uri: String, elementClass: Class<T>, vararg uriVariables: Any?): T = client.get()
    .uri(uri, *uriVariables)
    .retrieve()
    .bodyToMono(elementClass)
    .withRetryPolicy()
    .block()!!

  fun <T : Any> getIgnoreError(uri: String, elementClass: Class<T>, vararg uriVariables: Any?): T? = client.get()
    .uri(uri, *uriVariables)
    .retrieve()
    .bodyToMono(elementClass)
    .withRetryPolicy()
    .onErrorResume {
      log.warn("Unable to retrieve details due to {}", it.message)
      Mono.empty()
    }
    .block()

  fun <T : Any> getIfPresent(uri: String, elementClass: Class<T>, vararg uriVariables: Any?): T? = client.get()
    .uri(uri, *uriVariables)
    .retrieve()
    .bodyToMono(elementClass)
    .withRetryPolicy()
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
    .block()

  fun <T : Any> getWithParams(uri: String, elementClass: Class<T>, queryParams: Map<String, Any?>): T = client.get()
    .uri { uriBuilder -> uriBuilder.buildURI(uri, queryParams) }
    .retrieve()
    .bodyToMono(elementClass)
    .withRetryPolicy()
    .block()!!

  fun <T : Any> getWithParams(uri: String, elementClass: ParameterizedTypeReference<T>, queryParams: Map<String, Any?>): T = client.get()
    .uri { uriBuilder -> uriBuilder.buildURI(uri, queryParams) }
    .retrieve()
    .bodyToMono(elementClass)
    .withRetryPolicy()
    .block()!!

  fun patchWithBody(body: Any, uri: String, vararg uriVariables: Any?) {
    client.patch()
      .uri(uri, *uriVariables)
      .bodyValue(body)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun putWithBody(body: Any, uri: String, vararg uriVariables: Any?) {
    client.put()
      .uri(uri, *uriVariables)
      .bodyValue(body)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun put(uri: String, vararg uriVariables: Any?) {
    client.put()
      .uri(uri, *uriVariables)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun <T : Any> putWithResponse(uri: String, elementClass: Class<T>): T = client.put()
    .uri(uri)
    .retrieve()
    .bodyToMono(elementClass)
    .block()!!

  fun postWithBody(body: Any, uri: String, vararg uriVariables: Any?) {
    client.post()
      .uri(uri, *uriVariables)
      .bodyValue(body)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun <T : Any> postWithResponse(uri: String, body: Any, elementClass: Class<T>, vararg uriVariables: Any?): T = client.post()
    .uri(uri, *uriVariables)
    .bodyValue(body)
    .retrieve()
    .bodyToMono(elementClass)
    .block()!!

  fun <T : Any> postWithResponse(uri: String, body: Any, elementClass: ParameterizedTypeReference<T>, vararg uriVariables: Any?): T = client.post()
    .uri(uri, *uriVariables)
    .bodyValue(body)
    .retrieve()
    .bodyToMono(elementClass)
    .block()!!

  fun <T : Any> postWithResponse(uri: String, elementClass: Class<T>, vararg uriVariables: Any?): T = client.post()
    .uri(uri, *uriVariables)
    .retrieve()
    .bodyToMono(elementClass)
    .block()!!

  fun <T : Any> postWithResponse(uri: String, body: Any, elementClass: Class<T>, status: HttpStatus, newException: Exception): T = try {
    client.post()
      .uri(uri)
      .bodyValue(body)
      .retrieve()
      .bodyToMono(elementClass)
      .block()!!
  } catch (e: WebClientResponseException) {
    throw if (e.statusCode.equals(status)) newException else e
  }

  // todo Retry.max() creates a retry spec that does not apply exponential backoff between retry attempts.
  // This can lead to overwhelming the downstream service with rapid successive requests during transient
  // failures. Consider using Retry.backoff() instead to implement exponential backoff,
  // e.g.,
  // private fun <T> Mono<T>.withRetryPolicy(): Mono<T> = this
  //   .retryWhen(
  //     Retry.backoff(maxRetryAttempts, java.time.Duration.ofMillis(100))
  //       .filter { isTimeoutException(it) }
  //       .doBeforeRetry { logRetrySignal(it) },
  //   )

  private fun <T : Any> Mono<T>.withRetryPolicy(): Mono<T> = this
    .retryWhen(
      max(maxRetryAttempts)
        .filter { isTimeoutException(it) }
        .doBeforeRetry { logRetrySignal(it) },
    )

  private fun isTimeoutException(it: Throwable): Boolean {
    // Timeout for NO_RESPONSE is wrapped in a WebClientRequestException
    return it is ReadTimeoutException ||
      it is ConnectTimeoutException ||
      it.cause is ReadTimeoutException ||
      it.cause is ConnectTimeoutException
  }

  private fun logRetrySignal(retrySignal: Retry.RetrySignal) {
    val exception = retrySignal.failure()?.cause ?: retrySignal.failure()
    val message = exception.message ?: exception.javaClass.canonicalName
    log.debug("Retrying due to {}, totalRetries: {}", message, retrySignal.totalRetries())
  }

  fun delete(uri: String, vararg uriVariables: Any?) {
    client.delete()
      .uri(uri, *uriVariables)
      .retrieve()
      .toBodilessEntity()
      .block()
  }

  fun <T : Any> deleteWithResponse(uri: String, elementClass: Class<T>, vararg uriVariables: Any?): T = client.delete()
    .uri(uri, *uriVariables)
    .retrieve()
    .bodyToMono(elementClass)
    .block()!!

  private fun UriBuilder.buildURI(path: String, queryParams: Map<String, Any?>): URI {
    path(path)
    queryParams.forEach { (key, value) ->
      when (value) {
        null -> queryParam(key)
        is Collection<*> -> queryParam(key, *(value.filterNotNull().toTypedArray()))
        else -> queryParam(key, "{$key}")
      }
    }
    return build(queryParams)
  }
}

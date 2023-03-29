package uk.gov.justice.digital.hmpps.manageusersapi.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered.LOWEST_PRECEDENCE
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserExistsException
import uk.gov.justice.digital.hmpps.manageusersapi.service.EntityNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.HmppsValidationException
import javax.validation.ValidationException
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.ValidEmailException

@RestControllerAdvice
@Order(LOWEST_PRECEDENCE)
class HmppsManageUsersApiExceptionHandler {

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned with message {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .contentType(APPLICATION_JSON)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = e.message,
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(WebClientResponseException::class)
  fun handleWebClientResponseException(e: WebClientResponseException): ResponseEntity<ByteArray> {
    if (e.statusCode.is4xxClientError) {
      log.info("Unexpected client exception with message {}", e.message)
    } else {
      log.error("Unexpected server exception", e)
    }
    return ResponseEntity
      .status(e.rawStatusCode)
      .contentType(APPLICATION_JSON)
      .body(e.responseBodyAsByteArray)
  }

  @ExceptionHandler(UserExistsException::class)
  fun handleUserExistsException(e: UserExistsException): ResponseEntity<ErrorResponse?>? {
    log.debug("User exists exception caught: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .body(
        ErrorResponse(
          status = CONFLICT,
          errorCode = USER_EXISTS,
          userMessage = "User already exists",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(WebClientException::class)
  fun handleWebClientException(e: WebClientException): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .contentType(APPLICATION_JSON)
      .body(ErrorResponse(status = INTERNAL_SERVER_ERROR.value(), developerMessage = e.message))
  }

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
    return ResponseEntity
      .status(NOT_FOUND)
      .contentType(APPLICATION_JSON)
      .body(ErrorResponse(status = NOT_FOUND.value(), developerMessage = e.message))
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .contentType(APPLICATION_JSON)
      .body(ErrorResponse(status = INTERNAL_SERVER_ERROR.value(), developerMessage = e.message))
  }

  @ExceptionHandler(ValidEmailException::class)
  fun handleAuthUserLastGroupException(e: ValidEmailException): ResponseEntity<ErrorResponse> {
    log.info("Email validation exception caught: {}", e.message)
    return ResponseEntity.badRequest()
      .body(ErrorResponse(status = BAD_REQUEST.value(), developerMessage = e.message))
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .contentType(APPLICATION_JSON)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleValidationAnyException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse?>? {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .contentType(APPLICATION_JSON)
      .body(ErrorResponse(status = BAD_REQUEST, userMessage = e.message, developerMessage = e.message))
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse?>? {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .contentType(APPLICATION_JSON)
      .body(ErrorResponse(status = BAD_REQUEST, userMessage = e.message, developerMessage = e.message))
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse?>? {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .contentType(APPLICATION_JSON)
      .body(ErrorResponse(status = BAD_REQUEST, userMessage = e.message, developerMessage = e.message, errors = e.asErrorList()))
  }

  private fun MethodArgumentNotValidException.asErrorList(): List<String> =
    this.allErrors.mapNotNull { it.defaultMessage }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleValidationException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
    log.debug("Bad Request (400) returned", e)
    return ResponseEntity
      .status(BAD_REQUEST)
      .contentType(APPLICATION_JSON)
      .body(ErrorResponse(status = BAD_REQUEST.value(), developerMessage = e.message))
  }

  @ExceptionHandler(HmppsValidationException::class)
  fun handleCustomValidationException(e: HmppsValidationException): ResponseEntity<ErrorResponse?>? {
    log.debug("Validation exception caught: {}", e.message)
    return ResponseEntity
      .status(CONFLICT)
      .contentType(APPLICATION_JSON)
      .body(
        ErrorResponse(
          status = CONFLICT,
          errorCode = BASIC_VALIDATION_FAILURE,
          userMessage = e.message,
          developerMessage = e.message,
        ),
      )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

const val USER_EXISTS = 601
const val BASIC_VALIDATION_FAILURE = 602

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val errors: List<String>? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    errors: List<String>? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, errors)
}

package uk.gov.justice.digital.hmpps.manageusersapi.config

import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.NotFoundException

// Exception Handler to mirror the exceptions thrown by Auth.  Needed to ensure all endpoints moved from auth into this
// service return the same exception detail.
@RestControllerAdvice()
class AuthExceptionHandler {

  @ExceptionHandler(NotFoundException::class)
  fun handleNotFoundException(e: NotFoundException): ResponseEntity<ErrorDetail> {
    log.debug("Username not found exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(ErrorDetail(NOT_FOUND.reasonPhrase, e.message ?: "Error message not set", "username"))
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorDetail(
  @Schema(required = true, description = "Error", example = "Not Found")
  val error: String,

  @Schema(required = true, description = "Error description", example = "User not found.")
  val error_description: String,

  @Schema(description = "Field in error", example = "username")
  val field: String? = null,
)

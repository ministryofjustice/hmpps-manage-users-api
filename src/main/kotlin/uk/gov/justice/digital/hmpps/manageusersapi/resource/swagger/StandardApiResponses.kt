package uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@FailApiResponses
@ApiResponses(
  value = [
    ApiResponse(
      responseCode = "200",
      description = "OK",
      content = [Content(mediaType = "application/json")],
    ),
  ],
)
annotation class StandardApiResponses

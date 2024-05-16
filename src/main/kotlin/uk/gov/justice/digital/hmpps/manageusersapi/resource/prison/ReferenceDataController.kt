
package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.ReferenceDataService

@RestController("ReferenceDataController")
@Validated
@RequestMapping("/prisonusers", produces = [MediaType.APPLICATION_JSON_VALUE])
class ReferenceDataController(private val referenceDataService: ReferenceDataService) {

  @GetMapping("/reference-data/caseloads", produces = [MediaType.APPLICATION_JSON_VALUE])
  @Operation(
    summary = "Retrieves all caseloads",
    description = "Retrieves all the current active general caseloads, these are effectively prisons that staff can be associated with.",
    responses = [
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun getCaseload(): List<PrisonCaseload> = referenceDataService.getCaseloads()
}

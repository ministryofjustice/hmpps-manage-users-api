package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseloadDetail
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserCaseloadService
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload as PrisonCaseloadDomain

@RestController("PrisonUserCaseloadsController")
@Validated
@RequestMapping("/prisonusers", produces = [MediaType.APPLICATION_JSON_VALUE])
class UserCaseloadsController(
  private val userCaseloadService: UserCaseloadService,
) {
  @PreAuthorize("hasRole('ROLE_MAINTAIN_ACCESS_ROLES_ADMIN') or hasRole('ROLE_MAINTAIN_ACCESS_ROLES')")
  @GetMapping("/{username}/caseloads")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get list of caseloads associated with the users account",
    description = "Caseloads for a specific user. Requires role ROLE_MAINTAIN_ACCESS_ROLES_ADMIN or ROLE_MAINTAIN_ACCESS_ROLES\"",
    security = [SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES"), SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES_ADMIN")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "User caseload list",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to get caseloads for a user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to get a caseload for a user",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun getUserCaseloads(
    @Schema(description = "Username", example = "TEST_USER1", required = true)
    @PathVariable
    @Size(max = 30, min = 1, message = "username must be between 1 and 30")
    username: String,
  ): ResponseEntity<UserCaseloadDetail> {

    return ResponseEntity.ok(userCaseloadService.getUserCaseloads(username))
  }
}

data class PrisonCaseload(
  @Schema(description = "identify for caseload", example = "WWI")
  val id: String,
  @Schema(description = "name of caseload, typically prison name", example = "WANDSWORTH (HMP)")
  val name: String,
) {
  companion object {
    fun fromDomain(pcd: PrisonCaseloadDomain) =
      PrisonCaseload(pcd.id, pcd.name)
  }
}
package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.SyncService

@RestController
@Validated
class SyncController(
  private val syncService: SyncService,
) {
  @PostMapping("/prisonusers/{username}/email/sync")
  @PreAuthorize("hasAnyRole('ROLE_MAINTAIN_ACCESS_ROLES', 'ROLE_MAINTAIN_ACCESS_ROLES_ADMIN')")
  @Operation(
    summary = "Sync Nomis user email back into Auth",
    description = "Run process to check for differences in email address between Auth and NOMIS and updates Auth if required.<br/> " +
      "Requires role ROLE_MAINTAIN_ACCESS_ROLES or ROLE_MAINTAIN_ACCESS_ROLES_ADMIN",
    security = [SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES"), SecurityRequirement(name = "MAINTAIN_ACCESS_ROLES_ADMIN")],
  )
  @StandardApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "404",
        description = "User not found.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  fun syncUserEmail(
    @Parameter(description = "The username of the user.", required = true)
    @PathVariable
    username: String,
  ) = syncService.syncEmailWithNomis(username)
}

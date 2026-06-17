package uk.gov.justice.digital.hmpps.manageusersapi.resource.bulkjob

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.GroupSequence
import jakarta.validation.ValidationException
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.AcceptedApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob.BulkUserJobService
import java.util.UUID

@RestController
@RequestMapping("/bulk-jobs")
class BulkJobsController(private val bulkUserJobService: BulkUserJobService) {
  @PostMapping(path = ["/user-role-additions"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @PreAuthorize("hasRole('ROLE_MANAGE_USER_BULK_JOBS')")
  @Operation(
    summary = "Create a bulk user role additions job.",
    description = "Create a bulk user role additions job.",
  )
  @AcceptedApiResponses
  @ResponseStatus(HttpStatus.ACCEPTED)
  fun createUserRoleAdditionsJob(
    @RequestPart("userCsv", required = true) userCsv: MultipartFile,
    @Validated(ValidationOrder::class) @RequestPart("bulkJobDetails", required = true) bulkJobDetails: BulkUserRoleAdditionsRequest,
    authentication: Authentication,
  ): ResponseEntity<BulkUserRoleAdditionsResponse> {
    validateCsvFile(userCsv)
    val id = bulkUserJobService.createBulkUserRoleAdditionsJob(userCsv, bulkJobDetails, authentication.name)
    return ResponseEntity.accepted().body(BulkUserRoleAdditionsResponse(id = id))
  }

  private fun validateCsvFile(file: MultipartFile) {
    if (file.isEmpty) {
      throw ValidationException("Uploaded users file is empty")
    }
    if (!(file.originalFilename ?: "").endsWith(".csv", ignoreCase = true)) {
      throw ValidationException("Uploaded users file is not a CSV file")
    }
  }
}

@Schema(description = "Bulk user role additions request")
data class BulkUserRoleAdditionsRequest(
  @Schema(required = true, description = "JIRA reference", example = "ABC-1234")
  @field:NotBlank(message = "must be supplied", groups = [Required::class])
  @field:Size(min = 4, max = 266, message = "must be between 4 and 266 characters", groups = [Format::class])
  @JsonProperty("jiraReference")
  val jiraReference: String = "",

  @Schema(required = true, description = "JIRA reference", example = "ABC-1234")
  @field:NotEmpty(message = "must be supplied", groups = [Required::class])
  @JsonProperty("roles")
  val roles: List<String> = emptyList(),
)

@Schema(description = "Bulk user role additions response")
data class BulkUserRoleAdditionsResponse(
  @Schema(
    required = true,
    description = "Id of the bulk user role additions job",
    example = "123e4567-e89b-12d3-a456-426614174000",
  )
  val id: UUID,
)

interface Required
interface Format

@GroupSequence(Required::class, Format::class)
interface ValidationOrder

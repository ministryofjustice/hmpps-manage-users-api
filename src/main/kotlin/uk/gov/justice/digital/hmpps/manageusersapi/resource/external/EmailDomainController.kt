package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailDomain
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.CreateApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.FailApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.StandardApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.EmailDomainService
import java.util.UUID

@RestController
class EmailDomainController(
  private val emailDomainService: EmailDomainService,
) {

  @Operation(
    summary = "Get all email domains",
    description = "Get all email domains, role required is ROLE_MAINTAIN_EMAIL_DOMAINS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_EMAIL_DOMAINS")],
  )
  @StandardApiResponses
  @GetMapping("/email-domains")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun domainList(): List<EmailDomainDto> = emailDomainService.domainList().map { EmailDomainDto.fromDomain(it) }

  @Operation(
    summary = "Get email domain details",
    description = "Get email domain details, role required is ROLE_MAINTAIN_EMAIL_DOMAINS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_EMAIL_DOMAINS")],
  )
  @ApiResponses(
    ApiResponse(
      responseCode = "404",
      description = "Email domain not found",
      content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
    ),
  )
  @StandardApiResponses
  @GetMapping("/email-domains/{id}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun domain(@PathVariable id: UUID): EmailDomainDto = EmailDomainDto.fromDomain(emailDomainService.domain(id))

  @Operation(
    summary = "Create email domain",
    description = "Create a new email domain, role required is ROLE_MAINTAIN_EMAIL_DOMAINS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_EMAIL_DOMAINS")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = CreateEmailDomainDto::class),
        ),
      ],
    ),
  )
  @CreateApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "409",
        description = "Email domain already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/email-domains")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun addEmailDomain(
    @Schema(description = "Details of the email domain to be created.", required = true)
    @RequestBody
    @Valid
    emailDomain: CreateEmailDomainDto,
  ): EmailDomainDto = EmailDomainDto.fromDomain(emailDomainService.addEmailDomain(emailDomain))

  @Operation(
    summary = "Delete email domain details",
    description = "Delete email domain details, role required is ROLE_MAINTAIN_EMAIL_DOMAINS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_EMAIL_DOMAINS")],
  )
  @FailApiResponses
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Email domain details deleted",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Email domain not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/email-domains/{id}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun deleteEmailDomain(@PathVariable id: UUID) {
    emailDomainService.deleteEmailDomain(id)
  }
}

data class EmailDomainDto(
  @Schema(required = true, description = "Email domain id")
  val id: String,
  @Schema(required = true, description = "Email domain", example = "careuk.com")
  val domain: String,
  @Schema(required = false, description = "Email domain description", example = "CAREUK")
  val description: String,
) {
  companion object {
    fun fromDomain(emailDomain: EmailDomain) = EmailDomainDto(
      emailDomain.id,
      emailDomain.domain,
      emailDomain.description,
    )
  }
}

data class CreateEmailDomainDto(
  @Schema(required = true, description = "Email domain", example = "careuk.com")
  @field:NotBlank(message = "email domain name must be supplied")
  @field:Size(
    min = 6,
    max = 100,
    message = "email domain name must be between 6 and 100 characters in length (inclusive)",
  )
  val name: String = "",

  @Schema(required = false, description = "Email domain description", example = "CAREUK")
  @field:Size(max = 200, message = "email domain description cannot be greater than 200 characters in length")
  val description: String? = null,
)

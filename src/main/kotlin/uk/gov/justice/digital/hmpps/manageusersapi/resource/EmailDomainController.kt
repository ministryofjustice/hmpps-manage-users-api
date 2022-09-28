package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailDomainService
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@RestController
class EmailDomainController(
  private val emailDomainService: EmailDomainService
) {

  @Operation(
    summary = "Get all email domains",
    description = "Get all email domains, role required is ROLE_MAINTAIN_EMAIL_DOMAINS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_EMAIL_DOMAINS")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "All email domains returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = EmailDomainDto::class))
          )
        ]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_ROLES_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  @GetMapping(name = "/email-domains", produces = ["application/json"])
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun domainList(): List<EmailDomainDto> {
    return emailDomainService.domainList()
  }

  @Operation(
    summary = "Get email domain details",
    description = "Get email domain details, role required is ROLE_MAINTAIN_EMAIL_DOMAINS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_EMAIL_DOMAINS")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Email domain details returned",
        content = [
          Content(
            mediaType = "application/json",
            array = ArraySchema(schema = Schema(implementation = EmailDomainDto::class))
          )
        ]
      ),
      ApiResponse(
        responseCode = "404",
        description = "Email domain not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_ROLES_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  @GetMapping(name = "/email-domains/{id}", produces = ["application/json"])
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun domain(@PathVariable id: UUID): EmailDomainDto {
    return emailDomainService.domain(id)
  }

  @Operation(
    summary = "Create email domain",
    description = "Create a new email domain, role required is ROLE_MAINTAIN_EMAIL_DOMAINS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_EMAIL_DOMAINS")],
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [Content(mediaType = "application/json", schema = Schema(implementation = CreateEmailDomainDto::class))]
    ),
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Email domain created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = EmailDomainDto::class))]
      ),
      ApiResponse(
        responseCode = "409",
        description = "Email domain already exists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_ROLES_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(name = "/email-domains", produces = ["application/json"], consumes = ["application/json"])
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun addEmailDomain(
    @Schema(description = "Details of the email domain to be created.", required = true)
    @RequestBody @Valid emailDomain: CreateEmailDomainDto
  ): EmailDomainDto {
    return emailDomainService.addEmailDomain(emailDomain)
  }

  @Operation(
    summary = "Delete email domain details",
    description = "Delete email domain details, role required is ROLE_MAINTAIN_EMAIL_DOMAINS",
    security = [SecurityRequirement(name = "ROLE_MAINTAIN_EMAIL_DOMAINS")],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Email domain details deleted",
      ),
      ApiResponse(
        responseCode = "404",
        description = "Email domain not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint, requires a valid OAuth2 token",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an authorisation with role ROLE_ROLES_ADMIN",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      )
    ]
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
  val description: String
)

data class CreateEmailDomainDto(
  @Schema(required = true, description = "Email domain", example = "careuk.com")
  @field:NotBlank(message = "email domain name must be supplied")
  @field:Size(min = 6, max = 100, message = "email domain name must be between 6 and 100 characters in length (inclusive)")
  val name: String = "",

  @Schema(required = false, description = "Email domain description", example = "CAREUK")
  @field:Size(max = 200, message = "email domain description cannot be greater than 200 characters in length")
  val description: String? = null,
)

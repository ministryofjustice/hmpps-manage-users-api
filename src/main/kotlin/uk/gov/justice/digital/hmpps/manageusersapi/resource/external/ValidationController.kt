package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.resource.swagger.AuthenticatedApiResponses
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService

@RestController
@Validated
@Tag(name = "/validate", description = "Validation Controller")
class ValidationController(
  private val verifyEmailDomainService: VerifyEmailDomainService,
) {
  @GetMapping("/validate/email-domain")
  @Operation(
    summary = "Validates Email domain",
    description = "Validates Email domain.",
  )
  @AuthenticatedApiResponses
  fun isValidEmailDomain(@RequestParam(value = "emailDomain", required = true) emailDomain: String): Boolean =
    verifyEmailDomainService.isValidEmailDomain(emailDomain)
}

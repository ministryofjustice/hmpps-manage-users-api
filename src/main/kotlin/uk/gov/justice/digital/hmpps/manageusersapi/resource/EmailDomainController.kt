package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailDomainService
import java.util.UUID
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Controller
class EmailDomainController(
  private val emailDomainService: EmailDomainService
) {

  @GetMapping("/email-domains")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun domainList(): List<EmailDomainDto> {
    return emailDomainService.domainList()
  }

  @GetMapping("/email-domains/{id}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun domain(@PathVariable id: UUID): EmailDomainDto {
    return emailDomainService.domain(id)
  }

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/email-domains")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun addEmailDomain(@RequestBody @Valid emailDomain: CreateEmailDomainDto): EmailDomainDto {
    return emailDomainService.addEmailDomain(emailDomain)
  }

  @DeleteMapping("/email-domains/{id}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun deleteEmailDomain(@PathVariable id: UUID) {
    emailDomainService.deleteEmailDomain(id)
  }
}

data class EmailDomainDto(val id: String, val domain: String, val description: String)

data class CreateEmailDomainDto(
  @field:NotBlank(message = "email domain name must be supplied")
  @field:Size(min = 6, max = 100, message = "email domain name must be between 6 and 100 characters in length (inclusive)")
  val name: String = "",

  @field:Size(max = 200, message = "email domain description cannot be greater than 200 characters in length")
  val description: String? = null,
)

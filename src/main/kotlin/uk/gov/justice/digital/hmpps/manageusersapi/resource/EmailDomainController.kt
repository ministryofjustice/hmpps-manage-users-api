package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.servlet.ModelAndView
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailDomainService
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@Controller
class EmailDomainController(
  private val emailDomainService: EmailDomainService
){

  @GetMapping("/email-domains")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun domainList(): List<EmailDomainDto> {
    return emptyList()
  }

  @GetMapping("/email-domains/{id}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun domain(@PathVariable id: UUID): EmailDomainDto {
    return EmailDomainDto("", "", "")
  }

  @PostMapping("/email-domains")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun addEmailDomain(@Valid @ModelAttribute emailDomain: CreateEmailDomainDto, result: BindingResult) {

  }

  @DeleteMapping("/email-domains/{id}")
  @PreAuthorize("hasRole('ROLE_MAINTAIN_EMAIL_DOMAINS')")
  fun deleteEmailDomain(@PathVariable id: UUID){
  }

  private fun newEmailDomainView(createEmailDomainDto: CreateEmailDomainDto): ModelAndView {
    return ModelAndView("ui/newEmailDomainForm", "createEmailDomainDto", createEmailDomainDto)
  }

  private fun redirectToDomainListView(): ModelAndView {
    return ModelAndView("redirect:/email-domains")
  }

  private fun toDomainListView(emailDomains: List<EmailDomainDto>): ModelAndView {
    return ModelAndView("ui/emailDomains", mapOf("emailDomains" to emailDomains))
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

package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.EmailDomainDto
import java.util.UUID

@Service
class EmailDomainService(
  private val emailDomainApiService: EmailDomainApiService
) {
  fun domainList(): List<EmailDomainDto> {
    return emailDomainApiService.domainList()
  }

  fun domain(id: UUID): EmailDomainDto {
    return emailDomainApiService.domain(id)
  }

  fun addEmailDomain(emailDomain: CreateEmailDomainDto): EmailDomainDto {
    return emailDomainApiService.addEmailDomain(emailDomain)
  }

  fun deleteEmailDomain(id: UUID) {
    emailDomainApiService.deleteEmailDomain(id)
  }
}

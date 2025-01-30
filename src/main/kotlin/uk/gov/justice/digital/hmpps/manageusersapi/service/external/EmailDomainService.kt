package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.EmailDomainApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailDomain
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateEmailDomainDto
import java.util.UUID

@Service
class EmailDomainService(
  private val emailDomainApiService: EmailDomainApiService,
) {
  fun domainList(): List<EmailDomain> = emailDomainApiService.domainList()

  fun domain(id: UUID): EmailDomain = emailDomainApiService.domain(id)

  fun addEmailDomain(emailDomain: CreateEmailDomainDto): EmailDomain = emailDomainApiService.addEmailDomain(emailDomain)

  fun deleteEmailDomain(id: UUID) {
    emailDomainApiService.deleteEmailDomain(id)
  }
}

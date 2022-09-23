package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.EmailDomainDto
import java.util.UUID

@Service
class EmailDomainService(
  private val externalUsersEmailDomainApiService: ExternalUsersEmailDomainApiService
) {
  fun domainList(): List<EmailDomainDto> {
    return externalUsersEmailDomainApiService.domainList()
  }

  fun domain(id: UUID): EmailDomainDto {
    return externalUsersEmailDomainApiService.domain(id)
  }

  fun addEmailDomain(emailDomain: CreateEmailDomainDto): EmailDomainDto {
    return externalUsersEmailDomainApiService.addEmailDomain(emailDomain)
  }

  fun deleteEmailDomain(id: UUID) {
    externalUsersEmailDomainApiService.deleteEmailDomain(id)
  }
}

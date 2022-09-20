package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service

@Service
class EmailDomainService(
  private val externalUsersEmailDomainApiService: ExternalUsersEmailDomainApiService
) {
}
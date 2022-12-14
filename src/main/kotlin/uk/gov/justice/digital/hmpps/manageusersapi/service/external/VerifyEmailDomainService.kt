package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class VerifyEmailDomainService(val externalUsersApiService: ExternalUsersApiService) {
  fun isValidEmailDomain(emailDomain: String): Boolean =
    externalUsersApiService.validateEmailDomain(emailDomain)
}

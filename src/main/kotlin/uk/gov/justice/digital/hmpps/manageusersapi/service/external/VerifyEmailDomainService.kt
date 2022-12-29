package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers.VerifyEmailDomainApiService

@Service
@Transactional(readOnly = true)
class VerifyEmailDomainService(val verifyEmailDomainApiService: VerifyEmailDomainApiService) {

  fun isValidEmailDomain(emailDomain: String): Boolean =
    verifyEmailDomainApiService.validateEmailDomain(emailDomain)
}

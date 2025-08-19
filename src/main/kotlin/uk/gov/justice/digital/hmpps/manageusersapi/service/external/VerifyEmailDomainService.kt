package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.VerifyEmailDomainApiService

@Service
class VerifyEmailDomainService(val verifyEmailDomainApiService: VerifyEmailDomainApiService) {

  fun isValidEmailDomain(emailDomain: String): Boolean = verifyEmailDomainApiService.validateEmailDomain(emailDomain)
}

package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils

@Service
class VerifyEmailDomainApiService(
  @Qualifier("externalUsersWebClientUtils") val externalUsersWebClientUtils: WebClientUtils
) {

  fun validateEmailDomain(emailDomain: String) =
    externalUsersWebClientUtils.get("/validate/email-domain?emailDomain=$emailDomain", Boolean::class.java)
}

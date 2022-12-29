package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.EmailDomainDto
import java.util.UUID
import kotlin.collections.ArrayList

@Service
class EmailDomainApiService(
  @Qualifier("externalUsersWebClientUtils") val externalUsersWebClientUtils: WebClientUtils
) {

  fun domainList() = externalUsersWebClientUtils.get("/email-domains", EmailDomainList::class.java)

  fun domain(id: UUID) = externalUsersWebClientUtils.get("/email-domains/$id", EmailDomainDto::class.java)

  fun addEmailDomain(emailDomain: CreateEmailDomainDto) =
    externalUsersWebClientUtils.postWithResponse("/email-domains", emailDomain, EmailDomainDto::class.java)

  fun deleteEmailDomain(id: UUID) = externalUsersWebClientUtils.delete("/email-domains/$id")
}

class EmailDomainList : MutableList<EmailDomainDto> by ArrayList()

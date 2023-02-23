package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.EmailDomainDto
import java.util.UUID
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailDomain
import kotlin.collections.ArrayList

@Service
class EmailDomainApiService(
  @Qualifier("externalUsersUserWebClientUtils") val userWebClientUtils: WebClientUtils
) {

  fun domainList(): List<EmailDomainDto> = userWebClientUtils.get("/email-domains", EmailDomainList::class.java)

  fun domain(id: UUID) = userWebClientUtils.get("/email-domains/$id", EmailDomain::class.java)

  fun addEmailDomain(emailDomain: CreateEmailDomainDto) =
    userWebClientUtils.postWithResponse("/email-domains", emailDomain, EmailDomainDto::class.java)

  fun deleteEmailDomain(id: UUID) = userWebClientUtils.delete("/email-domains/$id")
}

class EmailDomainList : MutableList<EmailDomainDto> by ArrayList()

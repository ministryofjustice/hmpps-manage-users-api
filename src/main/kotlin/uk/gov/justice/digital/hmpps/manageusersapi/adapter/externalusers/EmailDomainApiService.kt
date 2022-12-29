package uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.EmailDomainDto
import java.util.UUID
import kotlin.collections.ArrayList

@Service
class EmailDomainApiService(
  @Qualifier("externalUsersWebClient") val externalUsersWebClient: WebClient
) {

  fun domainList(): List<EmailDomainDto> {
    return externalUsersWebClient.get()
      .uri("/email-domains")
      .retrieve()
      .bodyToMono(EmailDomainList::class.java)
      .block()!!
  }

  fun domain(id: UUID): EmailDomainDto {
    return externalUsersWebClient.get()
      .uri("/email-domains/$id")
      .retrieve()
      .bodyToMono(EmailDomainDto::class.java)
      .block()!!
  }

  fun addEmailDomain(emailDomain: CreateEmailDomainDto): EmailDomainDto {
    return externalUsersWebClient.post()
      .uri("/email-domains")
      .bodyValue(emailDomain)
      .retrieve()
      .bodyToMono(EmailDomainDto::class.java)
      .block()!!
  }

  fun deleteEmailDomain(id: UUID) {
    externalUsersWebClient.delete()
      .uri("/email-domains/$id")
      .retrieve()
  }

  class EmailDomainList : MutableList<EmailDomainDto> by ArrayList()
}

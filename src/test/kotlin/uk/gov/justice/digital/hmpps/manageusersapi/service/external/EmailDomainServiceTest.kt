package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.EmailDomainApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailDomain
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.EmailDomainDto
import java.util.UUID

internal class EmailDomainServiceTest {
  private val emailDomainApiService: EmailDomainApiService = mock()
  private val domainList: List<EmailDomainDto> = mock()
  private val domain: EmailDomainDto = mock()
  private val emailDomain: EmailDomain = mock()
  private val createEmailDomain: CreateEmailDomainDto = mock()
  private val emailDomainService = EmailDomainService(emailDomainApiService)

  @Test
  fun domainList() {
    whenever(emailDomainApiService.domainList()).thenReturn(domainList)

    val actual = emailDomainService.domainList()

    assertEquals(domainList, actual)
    verifyNoInteractions(domainList)
  }

  @Test
  fun domain() {
    val id = UUID.randomUUID()
    whenever(emailDomainApiService.domain(id)).thenReturn(emailDomain)

    val actual = emailDomainService.domain(id)

    assertEquals(emailDomain, actual)
    verifyNoInteractions(emailDomain)
  }

  @Test
  fun addEmailDomain() {
    whenever(emailDomainApiService.addEmailDomain(createEmailDomain)).thenReturn(domain)

    val actual = emailDomainService.addEmailDomain(createEmailDomain)

    assertEquals(domain, actual)
    verifyNoInteractions(domain)
  }

  @Test
  fun deleteEmailDomain() {
    val id = UUID.randomUUID()

    emailDomainService.deleteEmailDomain(id)

    verify(emailDomainApiService).deleteEmailDomain(id)
  }
}

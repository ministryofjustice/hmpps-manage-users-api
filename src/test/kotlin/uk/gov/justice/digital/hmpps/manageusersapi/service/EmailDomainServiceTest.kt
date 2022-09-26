package uk.gov.justice.digital.hmpps.manageusersapi.service

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.EmailDomainDto

internal class EmailDomainServiceTest {
  private val externalUsersEmailDomainApiService: ExternalUsersEmailDomainApiService = mock()
  private val domainList: List<EmailDomainDto> = mock()
  private val domain: EmailDomainDto = mock()
  private val createEmailDomain: CreateEmailDomainDto = mock()
  private val emailDomainService = EmailDomainService(externalUsersEmailDomainApiService)

    @Test
    fun domainList() {
      whenever(externalUsersEmailDomainApiService.domainList()).thenReturn(domainList)

      val actual = emailDomainService.domainList()

      assertEquals(domainList, actual)
      verifyNoInteractions(domainList)
    }

    @Test
    fun domain() {
      val id = UUID.randomUUID()
      whenever(externalUsersEmailDomainApiService.domain(id)).thenReturn(domain)

      val actual = emailDomainService.domain(id)

      assertEquals(domain, actual)
      verifyNoInteractions(domain)
    }

    @Test
    fun addEmailDomain() {
      whenever(externalUsersEmailDomainApiService.addEmailDomain(createEmailDomain)).thenReturn(domain)

      val actual = emailDomainService.addEmailDomain(createEmailDomain)

      assertEquals(domain, actual)
      verifyNoInteractions(domain)
    }

    @Test
    fun deleteEmailDomain() {
      val id = UUID.randomUUID()

      emailDomainService.deleteEmailDomain(id)

      verify(externalUsersEmailDomainApiService).deleteEmailDomain(id)
    }
}

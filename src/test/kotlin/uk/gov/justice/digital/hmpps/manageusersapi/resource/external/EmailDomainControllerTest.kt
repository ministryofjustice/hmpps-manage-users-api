package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateEmailDomainDto
import uk.gov.justice.digital.hmpps.manageusersapi.resource.EmailDomainController
import uk.gov.justice.digital.hmpps.manageusersapi.resource.EmailDomainDto
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.EmailDomainService
import java.util.UUID

internal class EmailDomainControllerTest {

  private val emailDomainService: EmailDomainService = mock()
  private val emailDomains: List<EmailDomainDto> = mock()
  private val emailDomain: EmailDomainDto = mock()
  private val emailDomainController = EmailDomainController(emailDomainService)
  private val createEmailDomain: CreateEmailDomainDto = mock()

  @Test
  fun domainListRetrieved() {
    whenever(emailDomainService.domainList()).thenReturn(emailDomains)

    val actual = emailDomainController.domainList()

    assertEquals(emailDomains, actual)
    verifyNoInteractions(emailDomains)
  }

  @Test
  fun domain() {
    val id = UUID.randomUUID()
    whenever(emailDomainService.domain(id)).thenReturn(emailDomain)

    val actual = emailDomainController.domain(id)

    assertEquals(emailDomain, actual)
    verifyNoInteractions(emailDomain)
  }

  @Test
  fun addEmailDomain() {
    whenever(emailDomainService.addEmailDomain(createEmailDomain)).thenReturn(emailDomain)

    val actual = emailDomainController.addEmailDomain(createEmailDomain)

    assertEquals(emailDomain, actual)
    verifyNoInteractions(emailDomain, createEmailDomain)
  }

  @Test
  fun deleteEmailDomain() {
    val id = UUID.randomUUID()

    emailDomainController.deleteEmailDomain(id)

    verify(emailDomainService).deleteEmailDomain(id)
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailDomain
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.EmailDomainService
import java.util.UUID

internal class EmailDomainControllerTest {

  private val emailDomainService: EmailDomainService = mock()
  private val emailDomains: List<EmailDomainDto> = mock()
  private val emailDomainController = EmailDomainController(emailDomainService)
  private val createEmailDomain: CreateEmailDomainDto = mock()

  private val emailDomainData = EmailDomain("1234", "testing.com", "testing")

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
    whenever(emailDomainService.domain(id)).thenReturn(emailDomainData)

    val actual = emailDomainController.domain(id)

    val expected = EmailDomainDto.fromDomain(emailDomainData)
    assertEquals(expected, actual)
  }

  @Test
  fun addEmailDomain() {
    whenever(emailDomainService.addEmailDomain(createEmailDomain)).thenReturn(emailDomainData)

    val actual = emailDomainController.addEmailDomain(createEmailDomain)

    val expected = EmailDomainDto.fromDomain(emailDomainData)
    assertEquals(expected, actual)
  }

  @Test
  fun deleteEmailDomain() {
    val id = UUID.randomUUID()

    emailDomainController.deleteEmailDomain(id)

    verify(emailDomainService).deleteEmailDomain(id)
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.VerifyEmailDomainApiService

class VerifyEmailDomainServiceTest {
  private val verifyEmailDomainApiService: VerifyEmailDomainApiService = mock()
  private val verifyService = VerifyEmailDomainService(verifyEmailDomainApiService)

  @Test
  fun shouldBeValidIfDomainMatches() {
    whenever(verifyEmailDomainApiService.validateEmailDomain(anyString())).thenReturn(true)
    assertTrue(verifyService.isValidEmailDomain("validDomain.com"))
  }

  @Test
  fun shouldNotBeValidIfDomainDoesntMatch() {
    whenever(verifyEmailDomainApiService.validateEmailDomain(anyString())).thenReturn(false)
    assertFalse(verifyService.isValidEmailDomain("validDomain.com"))
  }
}

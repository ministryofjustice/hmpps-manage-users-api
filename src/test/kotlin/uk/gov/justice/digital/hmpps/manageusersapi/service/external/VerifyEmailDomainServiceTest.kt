package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers.ExternalUsersApiService

class VerifyEmailDomainServiceTest {
  private val externalUsersService: ExternalUsersApiService = mock()
  private val verifyService = VerifyEmailDomainService(externalUsersService)

  @Test
  fun shouldBeValidIfDomainMatches() {
    whenever(externalUsersService.validateEmailDomain(anyString())).thenReturn(true)
    assertTrue(verifyService.isValidEmailDomain("validDomain.com"))
  }

  @Test
  fun shouldNotBeValidIfDomainDoesntMatch() {
    whenever(externalUsersService.validateEmailDomain(anyString())).thenReturn(false)
    assertFalse(verifyService.isValidEmailDomain("validDomain.com"))
  }
}

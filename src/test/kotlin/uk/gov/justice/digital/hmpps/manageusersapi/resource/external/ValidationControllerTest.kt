package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService

class ValidationControllerTest {
  private val verifyEmailDomainService: VerifyEmailDomainService = mock()
  private val validationController = ValidationController(verifyEmailDomainService)

  @Test
  fun validEmailDomain() {
    validationController.isValidEmailDomain("validEmailDomain")
    verify(verifyEmailDomainService).isValidEmailDomain("validEmailDomain")
  }
}

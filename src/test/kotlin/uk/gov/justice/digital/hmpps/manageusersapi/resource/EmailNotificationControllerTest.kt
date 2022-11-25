package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.EmailNotificationService

internal class EmailNotificationControllerTest {

  private val emailNotificationService: EmailNotificationService = mock()
  private val emailNotificationDto: EmailNotificationDto = mock()
  private val emailNotificationController = EmailNotificationController(emailNotificationService)

  @Test
  fun sendEnableEmail() {

    emailNotificationController.sendEnableEmail(emailNotificationDto)

    verify(emailNotificationService).sendEnableEmail(emailNotificationDto)
  }
}

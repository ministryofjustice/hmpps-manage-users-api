package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationMessage

internal class NotificationBannerServiceTest {
  private val notificationBannerService = NotificationBannerService()

  @Test
  fun `get notification banner message from file`() {
    assertThat(notificationBannerService.getNotificationMessage("test", "localTest")).isEqualTo(
      NotificationMessage(message = "Test banner message\n")
    )
  }

  @Test
  fun `get notification banner empty message from file`() {
    assertThat(notificationBannerService.getNotificationMessage("empty", "localTest")).isEqualTo(
      NotificationMessage(message = "")
    )
  }

  @Test
  fun `get notification banner file does not exist`() {
    assertThat(notificationBannerService.getNotificationMessage("none", "localTest")).isEqualTo(
      NotificationMessage(message = "")
    )
  }
}

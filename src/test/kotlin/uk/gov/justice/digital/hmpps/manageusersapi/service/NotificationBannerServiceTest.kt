package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationMessage
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationPage

internal class NotificationBannerServiceTest {
  private val notificationBannerService = NotificationBannerService(
    "Test banner message",
    "",
  )

  @Test
  fun `get notification banner message from environment variable`() {
    assertThat(notificationBannerService.getNotificationMessage(NotificationPage.ROLES)).isEqualTo(
      NotificationMessage(message = "Test banner message")
    )
  }

  @Test
  fun `get notification banner empty message from environment variable`() {
    assertThat(notificationBannerService.getNotificationMessage(NotificationPage.SEARCH)).isEqualTo(
      NotificationMessage(message = "")
    )
  }
}

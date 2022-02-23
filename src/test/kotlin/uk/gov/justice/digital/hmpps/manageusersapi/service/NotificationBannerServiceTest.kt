package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationMessage
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationPage

internal class NotificationBannerServiceTest {
  private val notificationBannerService = NotificationBannerService(
    "Roles test banner message",
    "",
    "DPS menu test banner message",
  )

  @Test
  fun `get roles notification banner message from environment variable`() {
    assertThat(notificationBannerService.getNotificationMessage(NotificationPage.ROLES)).isEqualTo(
      NotificationMessage(message = "Roles test banner message")
    )
  }

  @Test
  fun `get notification banner empty message from environment variable`() {
    assertThat(notificationBannerService.getNotificationMessage(NotificationPage.EMPTY)).isEqualTo(
      NotificationMessage(message = "")
    )
  }

  @Test
  fun `get DSP menu notification banner message from environment variable`() {
    assertThat(notificationBannerService.getNotificationMessage(NotificationPage.DPSMENU)).isEqualTo(
      NotificationMessage(message = "DPS menu test banner message")
    )
  }
}

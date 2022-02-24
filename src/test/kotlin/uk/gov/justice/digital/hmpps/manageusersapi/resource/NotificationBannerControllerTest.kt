package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.service.NotificationBannerService

internal class NotificationBannerControllerTest {

  private val notificationBannerService: NotificationBannerService = mock()
  private val notificationBannerController = NotificationBannerController(notificationBannerService)

  @Test
  fun `get Role Banner Message`() {
    whenever(notificationBannerService.getNotificationMessage(any())).thenReturn(NotificationMessage("BannerMessage"))
    val message = notificationBannerController.getNotificationBannerMessage(NotificationPage.ROLES)
    verify(notificationBannerService).getNotificationMessage(NotificationPage.ROLES)
    assertThat(message).isEqualTo(NotificationMessage(message = "BannerMessage"))
  }
}

enum class NotificationType(val filePrefix: String) {
  ROLES("roles"),
  TEST("test"),
  NOFILE("nofile"),
  EMPTY("empty"),
}

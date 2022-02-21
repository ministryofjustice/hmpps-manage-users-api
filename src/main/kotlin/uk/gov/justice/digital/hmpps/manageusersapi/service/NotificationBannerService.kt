package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.ResourceUtils.getFile
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NotificationMessage
import java.io.FileNotFoundException

@Service
class NotificationBannerService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getNotificationMessage(type: String, env: String): NotificationMessage {
    val message: String = try {
      getFile("classpath:${env}Banners/${type}BannerMessage.txt").readText(Charsets.UTF_8)
    } catch (e: FileNotFoundException) {
      log.debug("classpath:${env}Banners/${type}BannerMessage.txt")
      ""
    }

    return NotificationMessage(message)
  }
}

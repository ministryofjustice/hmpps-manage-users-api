package uk.gov.justice.digital.hmpps.manageusersapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication()
@ConfigurationPropertiesScan
@EnableSpringDataWebSupport
@EnableScheduling
class ManageUsersApi

fun main(args: Array<String>) {
  runApplication<ManageUsersApi>(*args)
}

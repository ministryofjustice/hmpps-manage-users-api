package uk.gov.justice.digital.hmpps.manageusersapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.web.config.EnableSpringDataWebSupport

@SpringBootApplication()
@ConfigurationPropertiesScan
@EnableSpringDataWebSupport
class ManageUsersApi

fun main(args: Array<String>) {
  runApplication<ManageUsersApi>(*args)
}

package uk.gov.justice.digital.hmpps.manageusersapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication()
@ConfigurationPropertiesScan
class ManageUsersApi

fun main(args: Array<String>) {
  runApplication<ManageUsersApi>(*args)
}

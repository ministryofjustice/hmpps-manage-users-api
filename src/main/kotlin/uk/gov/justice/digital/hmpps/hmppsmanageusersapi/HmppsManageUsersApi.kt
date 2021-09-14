package uk.gov.justice.digital.hmpps.hmppsmanageusersapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsManageUsersApi

fun main(args: Array<String>) {
  runApplication<HmppsManageUsersApi>(*args)
}

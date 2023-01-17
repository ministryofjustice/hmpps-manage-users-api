package uk.gov.justice.digital.hmpps.manageusersapi.model

enum class AuthSource(val description: String) {
  auth("External"),
  azuread("Microsoft Azure"),
  delius("Delius"),
  nomis("DPS"),
  none("None")
}

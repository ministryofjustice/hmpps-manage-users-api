package uk.gov.justice.digital.hmpps.manageusersapi.model

enum class AuthSource(val description: String) {
  @Suppress("ktlint:standard:enum-entry-name-case", "standard:enum-entry-name-case")
  auth("External"),

  @Suppress("ktlint:standard:enum-entry-name-case", "standard:enum-entry-name-case")
  azuread("Microsoft Azure"),

  @Suppress("ktlint:standard:enum-entry-name-case", "standard:enum-entry-name-case")
  delius("Delius"),

  @Suppress("ktlint:standard:enum-entry-name-case", "standard:enum-entry-name-case")
  nomis("DPS"),

  @Suppress("ktlint:standard:enum-entry-name-case", "standard:enum-entry-name-case")
  none("None"),
}

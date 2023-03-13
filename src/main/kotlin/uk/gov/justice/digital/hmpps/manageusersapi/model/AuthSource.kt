package uk.gov.justice.digital.hmpps.manageusersapi.model

enum class AuthSource(val description: String) {
  auth("External"), // ktlint-disable enum-entry-name-case
  azuread("Microsoft Azure"), // ktlint-disable enum-entry-name-case
  delius("Delius"), // ktlint-disable enum-entry-name-case
  nomis("DPS"), // ktlint-disable enum-entry-name-case
  none("None"), // ktlint-disable enum-entry-name-case
}

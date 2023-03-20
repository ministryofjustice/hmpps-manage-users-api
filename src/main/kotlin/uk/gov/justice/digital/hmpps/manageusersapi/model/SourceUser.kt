package uk.gov.justice.digital.hmpps.manageusersapi.model

interface SourceUser {
  val authSource: AuthSource
  fun toGenericUser(): GenericUser
  fun emailAddress(): EmailAddress
}

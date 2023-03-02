package uk.gov.justice.digital.hmpps.manageusersapi.model

interface SourceUser {
  fun toGenericUser(): GenericUser
}

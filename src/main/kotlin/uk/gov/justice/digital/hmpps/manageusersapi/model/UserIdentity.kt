package uk.gov.justice.digital.hmpps.manageusersapi.model

interface UserIdentity {
    val username: String
    val firstName: String
    val lastName: String
}

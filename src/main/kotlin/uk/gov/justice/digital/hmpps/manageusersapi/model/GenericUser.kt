package uk.gov.justice.digital.hmpps.manageusersapi.model

import java.util.UUID

data class GenericUser(
  val username: String,
  var active: Boolean,
  var name: String,
  var authSource: AuthSource,
  var staffId: Long? = null,
  var activeCaseLoadId: String? = null,
  var userId: String,
  var uuid: UUID? = null
)

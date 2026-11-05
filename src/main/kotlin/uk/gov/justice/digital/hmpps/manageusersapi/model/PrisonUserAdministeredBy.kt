package uk.gov.justice.digital.hmpps.manageusersapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Whether a local administrator looks after a user")
data class PrisonUserAdministeredBy(
  val username: String,
  val localAdministratorUsername: String,
  @Schema(description = "True if the user belongs to a local admin authority the local administrator manages")
  val administered: Boolean,
)

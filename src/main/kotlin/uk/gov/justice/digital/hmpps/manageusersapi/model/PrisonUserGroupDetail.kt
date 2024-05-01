package uk.gov.justice.digital.hmpps.manageusersapi.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User Group Information")
data class PrisonUserGroupDetail(
  val id: String,
  val name: String,
)

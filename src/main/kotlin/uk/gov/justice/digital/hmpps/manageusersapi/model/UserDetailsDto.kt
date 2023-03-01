package uk.gov.justice.digital.hmpps.manageusersapi.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageusersapi.service.User
import java.util.UUID

@JsonInclude(NON_NULL)
@Schema(description = "User Details")
data class UserDetailsDto(
  @Schema(description = "Username", example = "DEMO_USER1")
  override val username: String,

  @Schema(description = "Active", example = "false")
  var active: Boolean,

  @Schema(description = "Name", example = "John Smith")
  var name: String,

  @Schema(title = "Authentication Source", description = "auth for external users, nomis for nomis authenticated users", example = "nomis")
  var authSource: AuthSource,

  @Deprecated("")
  @Schema(title = "Staff Id", description = "Deprecated, use userId instead", example = "231232")
  var staffId: Long? = null,

  @Deprecated("")
  @Schema(title = "Current Active Caseload", description = "Deprecated, retrieve from prison API rather than manage users", example = "MDI")
  var activeCaseLoadId: String? = null,

  @Schema(title = "User Id", description = "Unique identifier for user, will be UUID for external users or staff ID for nomis users", example = "231232")
  var userId: String,

  @Schema(title = "Unique Id", description = "Universally unique identifier for user, generated and stored in auth database for all users", example = "5105a589-75b3-4ca0-9433-b96228c1c8f3")
  var uuid: UUID? = null

) : User {

  companion object {
    fun fromDomain(user: GenericUser): UserDetailsDto {
      with(user) {
        return UserDetailsDto(
          username,
          active,
          name,
          authSource,
          staffId,
          activeCaseLoadId,
          userId,
          uuid
        )
      }
    }
  }
}

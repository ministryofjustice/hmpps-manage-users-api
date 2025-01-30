package uk.gov.justice.digital.hmpps.manageusersapi.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource.nomis
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UsageType
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prison User Information")
data class PrisonUserDetails(
  @Schema(description = "Username", example = "testuser1", required = true) override val username: String,
  @Schema(description = "Staff ID", example = "324323", required = true) val staffId: Long,
  @Schema(description = "First name of the user", example = "John", required = true) override val firstName: String,
  @Schema(description = "Last name of the user", example = "Smith", required = true) override val lastName: String,
  @Schema(description = "Active Caseload of the user", example = "BXI", required = false) val activeCaseloadId: String?,
  @Schema(description = "Status of the user", example = "OPEN", required = false) val accountStatus: PrisonAccountStatus?,
  @Schema(description = "Type of user account", example = "GENERAL", required = true) val accountType: UsageType = UsageType.GENERAL,
  @Schema(description = "Email addresses of user", example = "test@test.com", required = false) val primaryEmail: String?,
  @Schema(description = "List of associated DPS Role Codes", required = false) val dpsRoleCodes: List<String>?,
  @Schema(description = "List of user groups administered", required = false) val administratorOfUserGroups: List<PrisonUserGroupDetail>?,
  @Schema(description = "Account is not locked", required = false) val accountNonLocked: Boolean?,
  @Schema(description = "Credentials are not expired flag", required = false) val credentialsNonExpired: Boolean?,
  @Schema(description = "User is enabled flag", required = true) val enabled: Boolean,
  @Schema(description = "User is admin flag", required = false) val admin: Boolean?,
  @Schema(description = "User is active flag", required = true) val active: Boolean?,
  @Schema(description = "Staff Status", example = "ACTIVE", required = false) val staffStatus: String?,
  @Schema(description = "Last logon date", example = "2023-01-01T12:13:14.123", required = false) val lastLogonDate: LocalDateTime?,
) : SourceUser,
  UserIdentity {
  val userId = staffId

  val name: String
    get() = "$firstName $lastName"

  override val authSource: AuthSource
    get() = nomis

  override fun toGenericUser(): GenericUser = GenericUser(
    username = username,
    active = enabled,
    authSource = nomis,
    userId = userId.toString(),
    name = name,
    uuid = null,
    staffId = userId.toLong(),
    activeCaseLoadId = activeCaseloadId,
  )

  override fun emailAddress(): EmailAddress = EmailAddress(username, primaryEmail, !primaryEmail.isNullOrEmpty())
}

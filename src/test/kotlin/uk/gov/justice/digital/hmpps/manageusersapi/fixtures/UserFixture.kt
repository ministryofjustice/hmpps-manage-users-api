package uk.gov.justice.digital.hmpps.manageusersapi.fixtures

import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonAccountStatus
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonAdminUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonStaffUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUsageType
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserBasicDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserSearchSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserSummary
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseloadDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.CaseloadRoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.RoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UsageType
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserRoleDetail
import java.time.LocalDateTime
import java.util.UUID

class UserFixture {

  companion object {
    fun createExternalUserDetails(
      userId: UUID,
      username: String = "testing",
      email: String = "testy@testing.com",
      firstName: String = "first",
      lastName: String = "last",
      locked: Boolean = false,
      enabled: Boolean = true,
      verified: Boolean = true,
      lastLoggedIn: LocalDateTime = LocalDateTime.now().minusDays(1),
      inactiveReason: String? = null,
    ) = ExternalUser(
      userId = userId,
      username = username,
      email = email,
      firstName = firstName,
      lastName = lastName,
      locked = locked,
      enabled = enabled,
      verified = verified,
      lastLoggedIn = lastLoggedIn,
      inactiveReason = inactiveReason,
    )

    fun createPrisonUserDetails(
      username: String = "NUSER_GEN",
      firstName: String = "Nomis",
      staffId: Int = 123456,
      lastName: String = "Take",
      activeCaseLoadId: String = "MDI",
      email: String = "nomis.usergen@digital.justice.gov.uk",
      enabled: Boolean = true,
      roles: List<String> = listOf("ROLE1", "ROLE2", "ROLE3"),
    ) = PrisonUser(
      username = username,
      firstName = firstName,
      staffId = staffId,
      lastName = lastName,
      activeCaseLoadId = activeCaseLoadId,
      email = email,
      enabled = enabled,
      roles = roles,
    )

    fun createPrisonUserFullDetails() = PrisonUserDetails(
      username = "NUSER_GEN",
      firstName = "Nomis",
      staffId = 123456,
      lastName = "Take",
      activeCaseloadId = "MDI",
      accountStatus = PrisonAccountStatus.OPEN,
      accountType = UsageType.GENERAL,
      primaryEmail = "bob@example.com",
      dpsRoleCodes = listOf("ROLE1", "ROLE2", "ROLE3"),
      administratorOfUserGroups = listOf(),
      accountNonLocked = true,
      credentialsNonExpired = true,
      enabled = true,
      admin = false,
      active = true,
      staffStatus = "ACTIVE",
      lastLogonDate = LocalDateTime.now(),
    )

    fun createPrisonUserBasicDetails(
      username: String = "NUSER_GEN",
      firstName: String = "Nomis",
      staffId: Int = 123456,
      lastName: String = "Take",
      activeCaseLoadId: String = "MDI",
      email: String = "nomis.usergen@digital.justice.gov.uk",
      enabled: Boolean = true,
      accountStatus: String = "OPEN",
    ) = PrisonUserBasicDetails(
      username = username,
      firstName = firstName,
      staffId = staffId,
      lastName = lastName,
      activeCaseLoadId = activeCaseLoadId,
      email = email,
      enabled = enabled,
      accountStatus = accountStatus,
    )

    fun createPrisonUserSummary(
      username: String = "NUSER_GEN",
      firstName: String = "Nomis",
      staffId: String = "123456",
      lastName: String = "Take",
      activeCaseLoad: PrisonCaseload = PrisonCaseload("MDI", "Moorland (HMP)"),
      email: String = "nomis.usergen@digital.justice.gov.uk",
      active: Boolean = true,
    ) = PrisonUserSummary(
      username = username,
      staffId = staffId,
      firstName = firstName,
      lastName = lastName,
      active = active,
      activeCaseload = activeCaseLoad,
      email = email,
    )

    fun createPrisonUserSearchSummary(
      username: String = "NUSER_GEN",
      staffId: Int = 123456,
      firstName: String = "Nomis",
      lastName: String = "Take",
      active: Boolean = true,
      status: String = "ACTIVE",
      locked: Boolean = false,
      expired: Boolean = false,
      activeCaseload: PrisonCaseload = PrisonCaseload("MDI", "Moorland (HMP)"),
      dpsRoleCount: Int = 2,
      email: String = "",
    ) = PrisonUserSearchSummary(
      username = username,
      staffId = staffId,
      firstName = firstName,
      lastName = lastName,
      active = active,
      status = status,
      locked = locked,
      expired = expired,
      activeCaseload = activeCaseload,
      dpsRoleCount = dpsRoleCount,
      email = email,
      staffStatus = "ACTIVE",
    )

    fun createPrisonAdminUserSummary(
      username: String = "NUSER_GEN",
      staffId: Long = 123456,
      firstName: String = "Nomis",
      lastName: String = "Take",
      active: Boolean = true,
      status: PrisonAccountStatus = PrisonAccountStatus.OPEN,
      locked: Boolean = false,
      expired: Boolean = false,
      activeCaseload: PrisonCaseload = PrisonCaseload("MDI", "Moorland (HMP)"),
      dpsRoleCount: Int = 2,
      email: String = "nomis.take@example.com",
    ) = PrisonAdminUserSummary(
      username = username,
      staffId = staffId,
      firstName = firstName,
      lastName = lastName,
      active = active,
      status = status,
      locked = locked,
      expired = expired,
      activeCaseload = activeCaseload,
      dpsRoleCount = dpsRoleCount,
      email = email,
      groups = listOf(),
      staffStatus = "ACTIVE",
    )

    fun createPrisonUserRoleDetails(
      username: String = "NUSER_GEN",
      active: Boolean = true,
      accountType: UsageType = UsageType.GENERAL,
    ) = UserRoleDetail(
      username = username,
      active = active,
      accountType = accountType,
      activeCaseload = uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.PrisonCaseload(
        "MDI",
        "Moorland (HMP)",
      ),
      dpsRoles = listOf(),
      nomisRoles = listOf(
        CaseloadRoleDetail(
          uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.PrisonCaseload("MDI", "Moorland (HMP)"),
          listOf(
            RoleDetail("ROLE1", "Role 1"),
            RoleDetail("ROLE2", "Role 2"),
          ),
        ),
      ),
    )

    fun createPrisonStaffUser(
      staffId: Long = 100,
      firstName: String = "First",
      lastName: String = "Last",
      status: String = "ACTIVE",
      primaryEmail: String = "f.l@justice.gov.uk",
      generalUserName: String = "TEST_USER_GEN",
      adminUserName: String = "TEST_USER_ADM",
    ): PrisonStaffUser {
      val generalCaseLoads = listOf(
        PrisonCaseload("NWEB", "Nomis-web Application"),
        PrisonCaseload("BXI", "Brixton (HMP)"),
      )
      val adminCaseLoads = listOf(
        PrisonCaseload("NWEB", "Nomis-web Application"),
        PrisonCaseload("CADM_I", "Central Administration Caseload For Hmps"),
      )
      val generalAccount = UserCaseload(
        generalUserName,
        false,
        PrisonUsageType.GENERAL,
        generalCaseLoads.get(1),
        generalCaseLoads,
      )
      val adminAccount =
        UserCaseload(
          adminUserName,
          false,
          PrisonUsageType.ADMIN,
          adminCaseLoads.get(1),
          adminCaseLoads,
        )

      return PrisonStaffUser(
        staffId = staffId,
        firstName = firstName,
        lastName = lastName,
        status = status,
        primaryEmail = primaryEmail,
        generalAccount,
        adminAccount,
      )
    }

    fun createPrisonUserCaseloadDetails(
      username: String = "NUSER_GEN",
      active: Boolean = true,
      accountType: UsageType = UsageType.GENERAL,
    ) = UserCaseloadDetail(
      username = username,
      active = active,
      accountType = accountType,
      activeCaseload = PrisonCaseload(id = "NWEB", name = "National Web"),
      caseloads = listOf(
        PrisonCaseload(
          id = "NWEB",
          name = "National Web",
        ),
        PrisonCaseload(
          id = "LEI",
          name = "Leeds",
        ),
      ),
    )
  }
}

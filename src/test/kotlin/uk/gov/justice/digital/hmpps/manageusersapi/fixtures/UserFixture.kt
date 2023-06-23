package uk.gov.justice.digital.hmpps.manageusersapi.fixtures

import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonStaffUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUsageType
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserBasicDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseload
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
  }
}

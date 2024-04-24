package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserRoleDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonCaseloadRole
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonRole
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonRoleType
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUsageType
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUserRole
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserRolesService

class UserRolesControllerTest {

  private val userRolesService: UserRolesService = mock()
  private val userRolesController = UserRolesController(userRolesService)

  @Test
  fun `get user roles`() {
    whenever(userRolesService.getUserRoles("SOME_USER")).thenReturn(createPrisonUserRole())

    val userRoles = userRolesController.getUserRoles("SOME_USER")
    verify(userRolesService).getUserRoles("SOME_USER")
    assertThat(userRoles).isEqualTo(UserRoleDetail.fromDomain(createPrisonUserRole()))
    verifyContentMatches(userRoles)
  }

  @Test
  fun `add user roles`() {
    val details = createPrisonUserRoleDetails()
    whenever(userRolesService.addRolesToUser(any(), any(), any())).thenReturn(details)

    val userRoles = userRolesController.addRoles("SOME_USER", "NWEB",listOf("role1", "role2"))

    verify(userRolesService).addRolesToUser("SOME_USER", listOf("role1", "role2"), "NWEB")
    assertThat(userRoles).isEqualTo(details)
  }

  private fun verifyContentMatches(actualUserRoles: UserRoleDetail) {
    val expectedRoles = createPrisonUserRole()
    with(expectedRoles) {
      assertThat(actualUserRoles.username).isEqualTo(username)
      assertThat(actualUserRoles.active).isEqualTo(active)
      assertThat(actualUserRoles.accountType.name).isEqualTo(accountType.name)
      assertThat(actualUserRoles.activeCaseload!!.id).isEqualTo(activeCaseload!!.id)
      assertThat(actualUserRoles.activeCaseload!!.name).isEqualTo(activeCaseload!!.name)
      assertThat(actualUserRoles.dpsRoles.size).isEqualTo(dpsRoles.size)
      assertThat(actualUserRoles.dpsRoles[0].name).isEqualTo(dpsRoles[0].name)
      assertThat(actualUserRoles.dpsRoles[0].code).isEqualTo(dpsRoles[0].code)
      assertThat(actualUserRoles.dpsRoles[0].sequence).isEqualTo(dpsRoles[0].sequence)
      assertThat(actualUserRoles.dpsRoles[0].type!!.name).isEqualTo(dpsRoles[0].type!!.name)
      assertThat(actualUserRoles.dpsRoles[0].adminRoleOnly).isEqualTo(dpsRoles[0].adminRoleOnly)
      assertThat(actualUserRoles.nomisRoles!!.size).isEqualTo(nomisRoles!!.size)
      assertThat(actualUserRoles.nomisRoles!![0].roles.size).isEqualTo(nomisRoles!![0].roles.size)
      assertThat(actualUserRoles.nomisRoles!![0].roles[0].name).isEqualTo(nomisRoles!![0].roles[0].name)
      assertThat(actualUserRoles.nomisRoles!![0].roles[0].code).isEqualTo(nomisRoles!![0].roles[0].code)
      assertThat(actualUserRoles.nomisRoles!![0].roles[0].sequence).isEqualTo(nomisRoles!![0].roles[0].sequence)
      assertThat(actualUserRoles.nomisRoles!![0].roles[0].type!!.name).isEqualTo(nomisRoles!![0].roles[0].type!!.name)
      assertThat(actualUserRoles.nomisRoles!![0].roles[0].adminRoleOnly).isEqualTo(nomisRoles!![0].roles[0].adminRoleOnly)
    }
  }

  private fun createPrisonUserRole(): PrisonUserRole {
    val activePrisonCaseload =
      PrisonCaseload("TESTING-1234", "TEST-CASELOAD-1")

    val prisonRoles = listOf(
      PrisonRole(
        "test-code",
        "test-role",
        1,
        PrisonRoleType.APP,
        false,
        null,
      ),
      PrisonRole(
        "test-code-2",
        "test-role-22",
        1,
        PrisonRoleType.APP,
        false,
        null,
      ),
    )

    val nomisRoles = listOf(
      PrisonCaseloadRole(
        activePrisonCaseload,
        prisonRoles,
      ),
    )

    return PrisonUserRole(
      "SOME_USER",
      true,
      PrisonUsageType.GENERAL,
      activePrisonCaseload,
      prisonRoles,
      nomisRoles,
    )
  }
}

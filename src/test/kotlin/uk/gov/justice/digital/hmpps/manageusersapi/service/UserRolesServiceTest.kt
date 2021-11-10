package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PrisonCaseload
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleType
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserRoleDetail

class UserRolesServiceTest {
  private val authService: AuthService = mock()
  private val nomisService: NomisApiService = mock()
  private val userRolesService = UserRolesService(authService, nomisService)

  @Test
  fun `get user roles`() {
    val userRolesFromNomis = createUserRoleDetails()
    val rolesFromAuth = listOf(
      Role(
        roleCode = "OMIC_ADMIN",
        roleName = "Key-worker allocator",
        roleDescription = null,
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      ),
      Role(
        roleCode = "MAINTAIN_ACCESS_ROLES",
        roleName = "Maintain DPS user roles",
        roleDescription = null,
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )
    )

    whenever(authService.getRoles(any())).thenReturn(rolesFromAuth)
    whenever(nomisService.getUserRoles(anyString())).thenReturn(userRolesFromNomis)
    val userRoles = userRolesService.getUserRoles("BOB")

    assertThat(userRoles).isEqualTo(userRolesFromNomis)
  }

  @Test
  fun `get user roles - RoleName is more than 30 characters so roleName take from auth`() {
    val userRolesFromNomis = createUserRoleDetails()
    val rolesFromAuth = listOf(
      Role(
        roleCode = "OMIC_ADMIN",
        roleName = "Key-worker allocator",
        roleDescription = null,
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      ),
      Role(
        roleCode = "MAINTAIN_ACCESS_ROLES",
        roleName = "Maintain access roles that has more than 30 characters in the role name",
        roleDescription = null,
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )
    )

    whenever(authService.getRoles(any())).thenReturn(rolesFromAuth)
    whenever(nomisService.getUserRoles(anyString())).thenReturn(userRolesFromNomis)
    val userRoles = userRolesService.getUserRoles("BOB")

    assertThat(userRoles).isNotEqualTo(userRolesFromNomis)
    assertThat(userRoles.dpsRoles[1].name).isEqualTo("Maintain access roles that has more than 30 characters in the role name")
  }

  @Test
  fun `get user roles - Roles are in alpha order by role name`() {
    val userRolesFromNomis = createUserRoleDetails()
    val rolesFromAuth = listOf(
      Role(
        roleCode = "MAINTAIN_ACCESS_ROLES",
        roleName = "Maintain access roles that has more than 30 characters in the role name",
        roleDescription = null,
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      ),
      Role(
        roleCode = "OMIC_ADMIN",
        roleName = "Key-worker allocator",
        roleDescription = null,
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )
    )

    whenever(authService.getRoles(any())).thenReturn(rolesFromAuth)
    whenever(nomisService.getUserRoles(anyString())).thenReturn(userRolesFromNomis)
    val userRoles = userRolesService.getUserRoles("BOB")

    assertThat(userRoles).isNotEqualTo(userRolesFromNomis)
    assertThat(userRoles.dpsRoles[0].name).isEqualTo("Key-worker allocator")
    assertThat(userRoles.dpsRoles[1].name).isEqualTo("Maintain access roles that has more than 30 characters in the role name")
  }

  private fun createUserRoleDetails() =
    UserRoleDetail(
      username = "bob",
      active = true,
      activeCaseload = PrisonCaseload(id = "CADM_I", name = "Central Administration Caseload For Hmps"),
      dpsRoles = listOf(
        RoleDetail(
          code = "OMIC_ADMIN",
          name = "Key-worker allocator",
          sequence = 1,
          type = RoleType.APP,
          adminRoleOnly = false
        ),
        RoleDetail(
          code = "MAINTAIN_ACCESS_ROLES",
          name = "Maintain DPS user roles",
          sequence = 1,
          type = RoleType.APP,
          adminRoleOnly = false
        )
      ),
      nomisRoles = listOf()
    )
}

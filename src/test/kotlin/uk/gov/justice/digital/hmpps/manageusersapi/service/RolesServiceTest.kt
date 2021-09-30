package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RoleNameAmendment

class RolesServiceTest {
  private val authService: AuthService = mock()
  private val nomisService: NomisApiService = mock()
  private val rolesService = RolesService(authService, nomisService)

  @Nested
  inner class AmendRoleName {
    @Test
    fun `update role name when DPS Role`() {
      val roleAmendment = RoleNameAmendment("UpdatedName")

      val dbRole = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
      )
      whenever(authService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleName("ROLE_1", roleAmendment)
      verify(authService).updateRoleName("ROLE_1", roleAmendment)
      verify(nomisService).updateRoleName("ROLE_1", roleAmendment)
    }

    @Test
    fun `update role name when Not DPS Role`() {
      val roleAmendment = RoleNameAmendment("UpdatedName")

      val dbRole = Role(
        roleCode = "ROLE_1", roleName = "Role Name", roleDescription = "A Role",
        adminType = listOf(AdminTypeReturn("EXT_ADM", "External Administrator"))
      )
      whenever(authService.getRoleDetail("ROLE_1")).thenReturn(dbRole)

      rolesService.updateRoleName("ROLE_1", roleAmendment)
      verify(authService).updateRoleName("ROLE_1", roleAmendment)
      verifyNoMoreInteractions(nomisService)
    }
  }
}

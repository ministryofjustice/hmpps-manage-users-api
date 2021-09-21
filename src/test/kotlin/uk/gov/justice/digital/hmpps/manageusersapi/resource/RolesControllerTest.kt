package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminTypeReturn
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.RolesService

class RolesControllerTest {

  private val rolesService: RolesService = mock()
  private val rolesController = RolesController(rolesService)

  @Test
  fun `Get role details`() {
    val role = Role(
      roleCode = "RO1",
      roleName = "Role1",
      roleDescription = "First Role",
      adminType = listOf(AdminTypeReturn("Code", "Name"))
    )

    whenever(rolesService.getRoleDetail(any())).thenReturn(role)

    val roleDetails = rolesController.getRoleDetail("RO1")
    assertThat(roleDetails).isEqualTo(
      Role(
        roleCode = "RO1",
        roleName = "Role1",
        roleDescription = "First Role",
        adminType = listOf(AdminTypeReturn("Code", "Name"))
      )
    )
  }

  @Test
  fun `Get role details with no match throws exception`() {
    whenever(rolesService.getRoleDetail(any())).thenThrow(RoleNotFoundException("find", "NoRole", "not found"))

    Assertions.assertThatThrownBy { rolesController.getRoleDetail("ROLE_DOES_NOT_EXIST") }
      .isInstanceOf(RoleNotFoundException::class.java)
      .withFailMessage("Unable to find role: NoRole with reason: not found")
  }
}

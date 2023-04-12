package uk.gov.justice.digital.hmpps.manageusersapi.resource.external

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.UserRolesService
import java.util.UUID

class UserRolesControllerTest {
  private val userRolesService: UserRolesService = mock()
  private val userRolesController = UserRolesController(userRolesService)

  @Test
  fun `get user roles`() {
    val userId = UUID.randomUUID()
    userRolesController.getUserRoles(userId)
    verify(userRolesService).getUserRoles(userId)
  }

  @Test
  fun `add roles to a user`() {
    val userId = UUID.randomUUID()
    userRolesController.addRolesByUserId(userId, listOf("role1", "role2"))
    verify(userRolesService).addRolesByUserId(userId, listOf("role1", "role2"))
  }

  @Test
  fun `add role to a user`() {
    val userId = UUID.randomUUID()
    userRolesController.addRoleByUserId(userId, "role1")
    verify(userRolesService).addRolesByUserId(userId, listOf("role1"))
  }

  @Test
  fun `remove a user role`() {
    val userId = UUID.randomUUID()
    userRolesController.removeRoleByUserId(userId, "ROLE_TEST")
    verify(userRolesService).removeRoleByUserId(userId, "ROLE_TEST")
  }

  @Test
  fun `get assignable user roles`() {
    val userId = UUID.randomUUID()
    userRolesController.getAssignableRoles(userId)
    verify(userRolesService).getAssignableRoles(userId)
  }
}

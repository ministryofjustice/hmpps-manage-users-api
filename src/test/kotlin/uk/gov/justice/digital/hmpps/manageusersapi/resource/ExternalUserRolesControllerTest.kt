package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.ExternalUserRolesService
import java.util.UUID

class ExternalUserRolesControllerTest {
  private val externalUserRolesService: ExternalUserRolesService = mock()
  private val externalUserRolesController = ExternalUserRolesController(externalUserRolesService)

  @Test
  fun `get user roles`() {
    val userId = UUID.randomUUID()
    externalUserRolesController.getUserRoles(userId)
    verify(externalUserRolesService).getUserRoles(userId)
  }

  @Test
  fun `remove a user role`() {
    val userId = UUID.randomUUID()
    externalUserRolesController.removeRoleByUserId(userId, "ROLE_TEST")
    verify(externalUserRolesService).removeRoleByUserId(userId, "ROLE_TEST")
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserRole
import java.util.UUID

class ExternalUserRolesServiceTest {
  private val externalUsersService: ExternalUsersApiService = mock()
  private val externalUserRolesService = ExternalUserRolesService(externalUsersService)

  @Test
  fun `get user roles`() {
    val rolesFromExternalUsers = listOf(
      UserRole(
        roleCode = "AUDIT_VIEWER",
        roleName = "viewer"
      ),
      UserRole(
        roleCode = "AUTH_GROUP_MANAGER",
        roleName = "Auth Group Manager that has more than 30 characters in the role name",
        roleDescription = "Gives group manager ability to administer user in there groups"
      )
    )
    whenever(externalUsersService.getUserRoles(any())).thenReturn(rolesFromExternalUsers)
    val userRoles = externalUserRolesService.getUserRoles(UUID.randomUUID())

    assertThat(userRoles[0].roleName).isEqualTo("viewer")
    assertThat(userRoles[1].roleName).isEqualTo("Auth Group Manager that has more than 30 characters in the role name")
  }
}

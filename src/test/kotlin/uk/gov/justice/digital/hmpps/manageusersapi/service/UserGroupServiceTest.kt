package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserGroup
import java.util.UUID

class UserGroupServiceTest {

  private val externalUsersService: ExternalUsersApiService = mock()
  private val userGroupService = UserGroupService(externalUsersService)

  @Test
  fun removeGroupByUserId() {
    val userId = UUID.randomUUID()
    userGroupService.removeGroupByUserId(userId, "test")
    verify(externalUsersService).deleteGroupByUserId(userId, "test")
  }

  @Test
  fun `get user groups with children`() {
    val groups = listOf(
      UserGroup(groupCode = "GROUP_ONE", groupName = "First Group"),
      UserGroup(groupCode = "GROUP_CHILD", groupName = "Child Group"),
    )
    val userId = UUID.randomUUID()
    whenever(externalUsersService.getUserGroups(userId, true)).thenReturn(groups)

    val userRoles = userGroupService.getUserGroups(userId, true)
    assertThat(userRoles).isEqualTo(groups)
  }

  @Test
  fun `get user groups without children`() {
    val groups = listOf(
      UserGroup(groupCode = "GROUP_ONE", groupName = "First Group"),
      UserGroup(groupCode = "GROUP_TWO", groupName = "Second Group"),
    )
    val userId = UUID.randomUUID()
    whenever(externalUsersService.getUserGroups(userId, false)).thenReturn(groups)

    val userRoles = userGroupService.getUserGroups(userId, false)
    assertThat(userRoles).isEqualTo(groups)
  }
}

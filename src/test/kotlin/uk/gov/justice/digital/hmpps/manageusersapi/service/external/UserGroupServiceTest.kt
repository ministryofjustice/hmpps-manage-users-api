package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserGroupApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserGroup
import java.util.UUID

class UserGroupServiceTest {

  private val userGroupApiService: UserGroupApiService = mock()
  private val userGroupService = UserGroupService(userGroupApiService)

  @Test
  fun removeGroupByUserId() {
    val userId = UUID.randomUUID()
    userGroupService.removeGroupByUserId(userId, "test")
    verify(userGroupApiService).deleteGroupByUserId(userId, "test")
  }

  @Test
  fun addGroupByUserId() {
    val userId = UUID.randomUUID()
    userGroupService.addGroupByUserId(userId, "test")
    verify(userGroupApiService).addGroupByUserId(userId, "test")
  }

  @Test
  fun `get user groups`() {
    val groups = givenAListOfGroups()
    val userId = UUID.randomUUID()
    whenever(userGroupApiService.getUserGroups(userId, false)).thenReturn(groups)

    val userRoles = userGroupService.getUserGroups(userId, false)
    assertThat(userRoles).isEqualTo(groups)
  }

  @Test
  fun `get my assignable groups`() {
    val myGroups = givenAListOfGroups()
    whenever(userGroupApiService.getMyAssignableGroups()).thenReturn(myGroups)

    assertThat(userGroupService.getMyAssignableGroups()).isEqualTo(myGroups)
  }

  private fun givenAListOfGroups(): List<UserGroup> = listOf(
    UserGroup(groupCode = "GROUP_ONE", groupName = "First Group"),
    UserGroup(groupCode = "GROUP_CHILD", groupName = "Child Group"),
  )
}

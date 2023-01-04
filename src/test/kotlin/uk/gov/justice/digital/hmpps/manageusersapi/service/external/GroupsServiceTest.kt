package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.GroupsApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ChildGroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.CreateGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.GroupDetails
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserAssignableRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserGroup

class GroupsServiceTest {
  private val groupsApiService: GroupsApiService = mock()
  private val groupsService = GroupsService(groupsApiService)

  @Test
  fun `Get all groups`() {
    val groups = listOf(UserGroup(groupCode = "FRED", groupName = "desc"))
    whenever(groupsApiService.getGroups()).thenReturn(groups)

    val actualGroups = groupsService.getGroups()
    assertThat(actualGroups).isEqualTo(groups)
    verify(groupsApiService).getGroups()
  }

  @Test
  fun `get group details`() {
    val groupDetails = GroupDetails(
      groupCode = "FRED",
      groupName = "desc",
      assignableRoles = listOf(UserAssignableRole(roleCode = "RO1", roleName = "Role1", automatic = true)),
      children = listOf(UserGroup(groupCode = "BOB", groupName = "desc"))
    )
    whenever(groupsApiService.getGroupDetail(anyString())).thenReturn(groupDetails)
    val group = groupsService.getGroupDetail("bob")

    assertThat(group).isEqualTo(groupDetails)
    verify(groupsApiService).getGroupDetail("bob")
  }

  @Test
  fun `get child group details`() {
    val childGroupDetails = ChildGroupDetails(groupCode = "CHILD_1", groupName = "Child - Site 1 - Group 2")
    whenever(groupsApiService.getChildGroupDetail(anyString())).thenReturn(childGroupDetails)

    val actualChildGroup = groupsService.getChildGroupDetail(childGroupDetails.groupCode)

    assertThat(actualChildGroup).isEqualTo(childGroupDetails)
    verify(groupsApiService).getChildGroupDetail(childGroupDetails.groupCode)
  }

  @Test
  fun `update child group details`() {
    val groupAmendment = GroupAmendment("Group Name")
    groupsService.updateChildGroup("code", groupAmendment)
    verify(groupsApiService).updateChildGroup("code", groupAmendment)
  }

  @Test
  fun `update group details`() {
    val groupAmendment = GroupAmendment("Group Name")
    groupsService.updateGroup("code", groupAmendment)
    verify(groupsApiService).updateGroup("code", groupAmendment)
  }

  @Test
  fun `Create group details`() {
    val createGroup = CreateGroup("Group Code", "Group Name")
    groupsService.createGroup(createGroup)
    verify(groupsApiService).createGroup(createGroup)
  }

  @Test
  fun `Delete child group`() {
    groupsService.deleteChildGroup("CHILD_1")
    verify(groupsApiService).deleteChildGroup("CHILD_1")
  }
  @Test
  fun `Delete group`() {
    groupsService.deleteGroup("group")
    verify(groupsApiService).deleteGroup("group")
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.service.GroupNotFoundException
import uk.gov.justice.digital.hmpps.manageusersapi.service.GroupsService

class GroupsControllerTest {

  private val groupsService: GroupsService = mock()
  private val groupsController = GroupsController(groupsService)

  @Nested
  inner class Groups {
    @Test
    fun `Get all Groups`() {
      val groups = listOf(UserGroup(groupCode = "BOB", groupName = "desc"))

      whenever(groupsService.getGroups()).thenReturn(groups)

      val response = groupsController.getGroups()
      assertThat(response).isEqualTo(
        groups
      )
    }
  }

  @Nested
  inner class GetGroups {
    @Test
    fun `Get Child Group details`() {
      val childGroupDetails = ChildGroupDetails("CHILD_1", "Child - Site 1 - Group 2")
      whenever(groupsService.getChildGroupDetail(childGroupDetails.groupCode)).thenReturn(childGroupDetails)

      val actualChildGroupDetail = groupsController.getChildGroupDetail(childGroupDetails.groupCode)

      assertThat(actualChildGroupDetail).isEqualTo(childGroupDetails)
      verify(groupsService).getChildGroupDetail(childGroupDetails.groupCode)
    }

    @Test
    fun `Get Group details`() {
      val groupsDetails =
        GroupDetails(
          groupCode = "FRED",
          groupName = "desc",
          assignableRoles = listOf(UserAssignableRole(roleCode = "RO1", roleName = "Role1", automatic = true)),
          children = listOf(UserGroup(groupCode = "BOB", groupName = "desc"))
        )

      whenever(
        groupsService.getGroupDetail(
          group = anyString()
        )
      ).thenReturn(groupsDetails)

      val response = groupsController.getGroupDetail("group")
      assertThat(response).isEqualTo(
        groupsDetails
      )
    }

    @Test
    fun `Group Not Found`() {
      whenever(groupsService.getGroupDetail("group")).thenThrow(GroupNotFoundException("find", "notfound", "not found"))

      assertThatThrownBy { groupsController.getGroupDetail("group") }
        .isInstanceOf(GroupNotFoundException::class.java)
        .withFailMessage("Unable to find group: notfound, reason: not found")
    }
  }

  @Nested
  inner class AmendGroupName {
    @Test
    fun `amend child group name`() {
      val groupAmendment = GroupAmendment("groupie")
      groupsController.amendChildGroupName("group1", groupAmendment)
      verify(groupsService).updateChildGroup("group1", groupAmendment)
    }

    @Test
    fun `amend group name`() {
      val groupAmendment = GroupAmendment("groupie")
      groupsController.amendGroupName("group1", groupAmendment)
      verify(groupsService).updateGroup("group1", groupAmendment)
    }
  }

  @Nested
  inner class CreateGroup {
    @Test
    fun createGroup() {
      val childGroup = CreateGroup("CG", "Group")
      groupsController.createGroup(childGroup)
      verify(groupsService).createGroup(childGroup)
    }

    @Test
    fun createChildGroup() {
      val childGroup = CreateChildGroup("PG", "CG", "Group")
      groupsController.createChildGroup(childGroup)
      verify(groupsService).createChildGroup(childGroup)
    }
  }

  @Nested
  inner class DeleteGroup {
    @Test
    fun deleteGroup() {
      groupsController.deleteGroup("GroupCode")
      verify(groupsService).deleteGroup("GroupCode")
    }

    @Test
    fun deleteChildGroup() {
      groupsController.deleteChildGroup("CHILD_1")

      verify(groupsService).deleteChildGroup("CHILD_1")
    }
  }
}

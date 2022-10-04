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
  inner class GetGroups {
    @Test
    fun `Get Group details`() {
      val groupsDetails =
        GroupDetails(
          groupCode = "FRED",
          groupName = "desc",
          assignableRoles = listOf(AuthUserAssignableRole(roleCode = "RO1", roleName = "Role1", automatic = true)),
          children = listOf(AuthUserGroup(groupCode = "BOB", groupName = "desc"))
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
    @Test
    fun `amend child group name`() {
      val groupAmendment = GroupAmendment("groupie")
      groupsController.amendChildGroupName("group1", groupAmendment)
      verify(groupsService).updateChildGroup("group1", groupAmendment)
    }
  }
}

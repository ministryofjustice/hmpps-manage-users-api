package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.resource.AuthUserAssignableRole
import uk.gov.justice.digital.hmpps.manageusersapi.resource.AuthUserGroup
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupAmendment
import uk.gov.justice.digital.hmpps.manageusersapi.resource.GroupDetails

class GroupsServiceTest {
  private val externalUsersService: ExternalUsersApiService = mock()
  private val groupsService = GroupsService(
    externalUsersService
  )

  @Test
  fun `get group details`() {
    val groupDetails = GroupDetails(
      groupCode = "FRED",
      groupName = "desc",
      assignableRoles = listOf(AuthUserAssignableRole(roleCode = "RO1", roleName = "Role1", automatic = true)),
      children = listOf(AuthUserGroup(groupCode = "BOB", groupName = "desc"))
    )
    whenever(externalUsersService.getGroupDetail(anyString())).thenReturn(groupDetails)
    val group = groupsService.getGroupDetail("bob")

    assertThat(group).isEqualTo(groupDetails)
    verify(externalUsersService).getGroupDetail("bob")
  }

  @Test
  fun `update child group details`() {

    val groupAmendment = GroupAmendment("Group Name")
    groupsService.updateChildGroup("code", groupAmendment)
    verify(externalUsersService).updateChildGroup("code", groupAmendment)
  }
}

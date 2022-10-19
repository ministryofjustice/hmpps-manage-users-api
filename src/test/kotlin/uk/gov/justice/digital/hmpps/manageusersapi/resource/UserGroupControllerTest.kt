package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserGroupService
import java.util.UUID.randomUUID

class UserGroupControllerTest {

  private val userGroupService: UserGroupService = mock()
  private val userGroupController = UserGroupController(userGroupService)

  @Test
  fun removeGroupByUserId() {
    val userId = randomUUID()
    userGroupController.removeGroupByUserId(userId, "test")
    verify(userGroupService).removeGroupByUserId(userId, "test")
  }

  @Test
  fun addGroupByUserId() {
    val userId = randomUUID()
    userGroupController.addGroupByUserId(userId, "test")
    verify(userGroupService).addGroupByUserId(userId, "test")
  }

  @Test
  fun `get user groups with children`() {
    val userId = randomUUID()
    userGroupController.getGroups(userId)
    verify(userGroupService).getUserGroups(userId, true)
  }

  @Test
  fun `get user groups without children`() {
    val userId = randomUUID()
    userGroupController.getGroups(userId, false)
    verify(userGroupService).getUserGroups(userId, false)
  }
}

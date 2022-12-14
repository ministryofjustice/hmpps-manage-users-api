package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.ExternalUserGroupService
import java.util.UUID.randomUUID

class ExternalUserGroupControllerTest {

  private val userGroupService: ExternalUserGroupService = mock()
  private val externalUserGroupController = ExternalUserGroupController(userGroupService)

  @Test
  fun removeGroupByUserId() {
    val userId = randomUUID()
    externalUserGroupController.removeGroupByUserId(userId, "test")
    verify(userGroupService).removeGroupByUserId(userId, "test")
  }

  @Test
  fun addGroupByUserId() {
    val userId = randomUUID()
    externalUserGroupController.addGroupByUserId(userId, "test")
    verify(userGroupService).addGroupByUserId(userId, "test")
  }

  @Test
  fun `get user groups with children`() {
    val userId = randomUUID()
    externalUserGroupController.getGroups(userId)
    verify(userGroupService).getUserGroups(userId, true)
  }

  @Test
  fun `get user groups without children`() {
    val userId = randomUUID()
    externalUserGroupController.getGroups(userId, false)
    verify(userGroupService).getUserGroups(userId, false)
  }
}

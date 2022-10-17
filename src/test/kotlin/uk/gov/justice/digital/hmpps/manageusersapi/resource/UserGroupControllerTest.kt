package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserGroupService
import java.util.UUID

class UserGroupControllerTest {

  private val userGroupService: UserGroupService = mock()
  private lateinit var userGroupController: UserGroupController

  @BeforeEach
  fun setUp() {
    userGroupController = UserGroupController(userGroupService)
  }

  @Test
  fun removeGroupByUserId() {
    val userId = UUID.randomUUID()
    userGroupController.removeGroupByUserId(userId, "test")
    verify(userGroupService).removeGroupByUserId(userId, "test")
  }
}

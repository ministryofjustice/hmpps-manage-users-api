package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleSyncService
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserSyncService

class SyncControllerTest {

  private val roleSyncService: RoleSyncService = mock()
  private val userSyncService: UserSyncService = mock()
  private val syncController = SyncController(roleSyncService, userSyncService)

  @Nested
  inner class SyncRoles {
    @Test
    fun `sync roles`() {
      syncController.syncRoles()
      verify(roleSyncService).sync(false)
    }
  }

  @Nested
  inner class SyncUsers {
    @Test
    fun `sync users`() {
      syncController.syncUsers()
      verify(userSyncService).sync()
    }
  }
}

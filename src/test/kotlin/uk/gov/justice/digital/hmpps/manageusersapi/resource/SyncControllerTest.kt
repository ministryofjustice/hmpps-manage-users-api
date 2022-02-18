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
      syncController.syncUsers(caseSensitive = false, usePrimaryEmail = false)
      verify(userSyncService).sync(caseSensitive = false, usePrimaryEmail = false)
    }

    @Test
    fun `sync users with case sensitive emails`() {
      syncController.syncUsers(caseSensitive = true, usePrimaryEmail = false)
      verify(userSyncService).sync(caseSensitive = true, usePrimaryEmail = false)
    }

    @Test
    fun `sync users with primary emails only`() {
      syncController.syncUsers(caseSensitive = false, usePrimaryEmail = true)
      verify(userSyncService).sync(caseSensitive = false, usePrimaryEmail = true)
    }
  }
}

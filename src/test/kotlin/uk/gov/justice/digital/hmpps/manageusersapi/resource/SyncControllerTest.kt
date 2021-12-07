package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleSyncService

class SyncControllerTest {

  private val roleSyncService: RoleSyncService = mock()
  private val syncController = SyncController(roleSyncService)

  @Nested
  inner class SyncRoles {
    @Test
    fun `sync roles`() {
      syncController.syncRoles()
      verify(roleSyncService).sync(false)
    }
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.resource

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleSyncService

class SyncControllerTest {

  private val roleSyncService: RoleSyncService = mock()
  private val syncController = SyncController(roleSyncService)

  @Nested
  inner class SyncRoles {
    @Test
    fun `sync roles`() {
      syncController.syncRoles()
      verify(roleSyncService).sync()
    }
  }
}

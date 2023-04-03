package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.SyncService

class SyncControllerTest {

  private val syncService: SyncService = mock()
  private val syncController = SyncController(syncService)

  @Nested
  inner class SyncUser {
    @Test
    fun `create DPS user`() {
      syncController.syncUserEmail("username")
      verify(syncService).syncEmailWithNomis("username")
    }
  }
}

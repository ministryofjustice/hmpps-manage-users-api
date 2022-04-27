package uk.gov.justice.digital.hmpps.manageusersapi.resource

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleSyncService
import uk.gov.justice.digital.hmpps.manageusersapi.service.SyncOptions
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
    fun `sync users`(): Unit = runBlocking {
      syncController.syncUsers(caseSensitive = false, usePrimaryEmail = false, onlyVerified = false, domainFilters = emptySet())
      verify(userSyncService).sync(SyncOptions(caseSensitive = false, usePrimaryEmail = false, onlyVerified = false, domainFilters = emptySet()))
    }

    @Test
    fun `sync users with case sensitive emails`(): Unit = runBlocking {
      syncController.syncUsers(caseSensitive = true, usePrimaryEmail = false, onlyVerified = false, domainFilters = emptySet())
      verify(userSyncService).sync(SyncOptions(caseSensitive = true, usePrimaryEmail = false, onlyVerified = false, domainFilters = emptySet()))
    }

    @Test
    fun `sync users with primary emails only`(): Unit = runBlocking {
      syncController.syncUsers(caseSensitive = false, usePrimaryEmail = true, onlyVerified = false, domainFilters = emptySet())
      verify(userSyncService).sync(SyncOptions(caseSensitive = false, usePrimaryEmail = true, onlyVerified = false, domainFilters = emptySet()))
    }

    @Test
    fun `sync users with verified emails only`(): Unit = runBlocking {
      syncController.syncUsers(caseSensitive = false, usePrimaryEmail = false, onlyVerified = true, domainFilters = emptySet())
      verify(userSyncService).sync(SyncOptions(caseSensitive = false, usePrimaryEmail = false, onlyVerified = true, domainFilters = emptySet()))
    }

    @Test
    fun `sync users with matching email domains only`(): Unit = runBlocking {
      syncController.syncUsers(caseSensitive = false, usePrimaryEmail = false, onlyVerified = true, domainFilters = setOf("digital.justice.gov.uk"))
      verify(userSyncService).sync(SyncOptions(caseSensitive = false, usePrimaryEmail = false, onlyVerified = true, domainFilters = setOf("digital.justice.gov.uk")))
    }
  }
}

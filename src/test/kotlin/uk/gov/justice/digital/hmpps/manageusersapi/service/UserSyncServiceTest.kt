package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.config.GsonConfig

class UserSyncServiceTest {
  private val authService: AuthService = mock()
  private val nomisService: NomisApiService = mock()
  private val gson: Gson = GsonConfig().gson()

  private val userSyncService = UserSyncService(nomisService, authService, gson)

  @Test
  fun `sync users that match`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username1", "user1@digital.justice.gov.uk"),
      NomisUser("username2", "user2@digital.justice.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val statistics = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getUsers()
    verifyNoMoreInteractions(nomisService)
    verifyNoMoreInteractions(authService)
    assertThat(statistics.results.size).isEqualTo(0)
  }

  @Test
  fun `sync users that match if not caseSensitive`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username1", "uSer1@digItal.justice.gov.uk"),
      NomisUser("username2", "useR2@digitaL.justice.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val statistics = userSyncService.sync(SyncOptions(caseSensitive = false))
    verify(authService).getUsers()
    verify(nomisService).getUsers()
    verifyNoMoreInteractions(nomisService)
    verifyNoMoreInteractions(authService)
    assertThat(statistics.results.size).isEqualTo(0)
  }

  @Test
  fun `sync users that don't case match if primaryEmail`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username1", "user1@gsi.gov.uk, uSer1@digItal.justice.gov.uk"),
      NomisUser("username2", "user2@digital.justice.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val statistics = userSyncService.sync(SyncOptions(caseSensitive = true, usePrimaryEmail = true))
    verify(authService).getUsers()
    verify(nomisService).getUsers()
    verifyNoMoreInteractions(nomisService)
    verifyNoMoreInteractions(authService)
    assertThat(statistics.results.size).isEqualTo(1)
  }

  @Test
  fun `sync users that match if primaryEmail`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username1", "user1@gsi.gov.uk, uSer1@digItal.justice.gov.uk"),
      NomisUser("username2", "user2@digitaL.justice.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val statistics = userSyncService.sync(SyncOptions(caseSensitive = false, usePrimaryEmail = true))
    verify(authService).getUsers()
    verify(nomisService).getUsers()
    verifyNoMoreInteractions(nomisService)
    verifyNoMoreInteractions(authService)
    assertThat(statistics.results.size).isEqualTo(0)
  }

  @Test
  fun `sync users that don't match if caseSensitive`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username1", "uSer1@digItal.justice.gov.uk"),
      NomisUser("username2", "useR2@digitaL.justice.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getUsers()
    verifyNoMoreInteractions(nomisService)
    verifyNoMoreInteractions(authService)
    assertThat(stats.results.size).isEqualTo(2)
    assertThat(stats.results["username1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username1"]?.differences).isEqualTo(
      "not equal: value differences={email=(uSer1@digItal.justice.gov.uk, user1@digital.justice.gov.uk)}"
    )
    assertThat(stats.results["username2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username2"]?.differences).isEqualTo(
      "not equal: value differences={email=(useR2@digitaL.justice.gov.uk, user2@digital.justice.gov.uk)}"
    )
  }

  @Test
  fun `sync users that have different email address`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username1", "user1@digital.justice.gov.uk"),
      NomisUser("username2", "user2nomis@digital.justice.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getUsers()

    // Nothing for username1 as there are no differences
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["username2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username2"]?.differences).isEqualTo(
      "not equal: value differences={email=(user2nomis@digital.justice.gov.uk, user2@digital.justice.gov.uk)}"
    )
  }

  @Test
  fun `sync user that is missing from Nomis`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1auth@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username2", "user2@digital.justice.gov.uk")
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getUsers()

    // Nothing for username2 as there are no differences
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["username1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username1"]?.differences).isEqualTo(
      "not equal: only on right={userName=username1, email=user1auth@digital.justice.gov.uk}"
    )
  }

  @Test
  fun `sync user that is missing from Auth`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk", false),
    )

    val usersFromNomis = listOf(
      NomisUser("username1", "user1@digital.justice.gov.uk"),
      NomisUser("username2", "user2nomis@digital.justice.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getUsers()

    // Nothing for username2 as it is missing from auth - we ignore username1 as it is only in nomis
    assertThat(stats.results.size).isEqualTo(0)
  }

  @Test
  fun `sync users with multiple differences`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1auth@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
      AuthUser("username3", "user3auth@digital.justice.gov.uk", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username2", "user2@digital.justice.gov.uk"),
      NomisUser("username3", "user3@digital.justice.gov.uk"),
    )

    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getUsers()

    // Nothing for username2 as there are no differences
    assertThat(stats.results.size).isEqualTo(2)
    assertThat(stats.results["username1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username1"]?.differences).isEqualTo("not equal: only on right={userName=username1, email=user1auth@digital.justice.gov.uk}")
    assertThat(stats.results["username3"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username3"]?.differences).isEqualTo(
      "not equal: value differences={email=(user3@digital.justice.gov.uk, user3auth@digital.justice.gov.uk)}"
    )
  }

  @Test
  fun `sync user that is has no email in Nomis`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1auth@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username1"),
      NomisUser("username2", "user2@digital.justice.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getUsers()

    // Nothing for username2 as there are no differences
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["username1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username1"]?.differences).isEqualTo(
      "not equal: only on right={email=user1auth@digital.justice.gov.uk}"
    )
  }

  @Test
  fun `sync user that is has no email in Auth`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser(username = "username1", verified = false),
      AuthUser("username2", "user2@digital.justice.gov.uk", false),
    )

    val usersFromNomis = listOf(
      NomisUser("username1", "user1nomis@digital.justice.gov.uk"),
      NomisUser("username2", "user2@digital.justice.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getUsers()

    // Nothing for username2 as there are no differences
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["username1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username1"]?.differences).isEqualTo(
      "not equal: only on left={email=user1nomis@digital.justice.gov.uk}"
    )
  }

  @Test
  fun `sync users only if verified in auth`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk", false),
      AuthUser("username2", "user2@digital.justice.gov.uk", true),
    )
    val usersFromNomis = listOf(
      NomisUser("username1", "user1@gsi.gov.uk"),
      NomisUser("username2", "user2@gsi.gov.uk"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val statistics = userSyncService.sync(SyncOptions(onlyVerified = true))
    verify(authService).getUsers()
    verify(nomisService).getUsers()
    verifyNoMoreInteractions(nomisService)
    verifyNoMoreInteractions(authService)
    assertThat(statistics.results.size).isEqualTo(1)
    assertThat(statistics.results["username2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(statistics.results["username2"]?.differences).isEqualTo(
      "not equal: value differences={email=(user2@gsi.gov.uk, user2@digital.justice.gov.uk)}"
    )
  }

  @Test
  fun `sync users only if auth email matches domain`(): Unit = runBlocking {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@domain1.com", false),
      AuthUser("username2", "user2@domain2.com", false),
      AuthUser("username3", "user3@sub.domain1.com", false),
    )
    val usersFromNomis = listOf(
      NomisUser("username1", "user1@other1.com"),
      NomisUser("username2", "user2@other2.com"),
      NomisUser("username3", "user3@other3.com"),
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getUsers()).thenReturn(usersFromNomis)

    val statistics = userSyncService.sync(SyncOptions(domainFilters = setOf("domain1.com")))
    verify(authService).getUsers()
    verify(nomisService).getUsers()
    verifyNoMoreInteractions(nomisService)
    verifyNoMoreInteractions(authService)
    assertThat(statistics.results.size).isEqualTo(1)
    assertThat(statistics.results["username1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(statistics.results["username1"]?.differences).isEqualTo(
      "not equal: value differences={email=(user1@other1.com, user1@domain1.com)}"
    )
  }
}

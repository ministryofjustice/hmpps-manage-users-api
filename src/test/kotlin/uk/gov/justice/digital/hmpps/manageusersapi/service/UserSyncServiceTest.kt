package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.google.gson.Gson
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
  fun `sync users that match`() {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk"),
      AuthUser("username2", "user2@digital.justice.gov.uk")
    )

    val usersFromNomis = listOf(
      NomisUser("username1", "user1@digital.justice.gov.uk"),
      NomisUser("username2", "user2@digital.justice.gov.uk")
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getAllUsers()).thenReturn(usersFromNomis)

    val statistics = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getAllUsers()
    verifyNoMoreInteractions(nomisService)
    verifyNoMoreInteractions(authService)
    assertThat(statistics.results.size).isEqualTo(0)
  }

  @Test
  fun `sync users that have different email address`() {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk"),
      AuthUser("username2", "user2@digital.justice.gov.uk")
    )

    val usersFromNomis = listOf(
      NomisUser("username1", "user1@digital.justice.gov.uk"),
      NomisUser("username2", "user2nomis@digital.justice.gov.uk")
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getAllUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getAllUsers()

    // Nothing for username1 as there are no differences
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["username2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username2"]?.differences).isEqualTo(
      "not equal: value differences={email=(user2nomis@digital.justice.gov.uk, user2@digital.justice.gov.uk)}"
    )
  }

  @Test
  fun `sync user that is missing from Nomis`() {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1auth@digital.justice.gov.uk"),
      AuthUser("username2", "user2@digital.justice.gov.uk"),
    )

    val usersFromNomis = listOf(
      NomisUser("username2", "user2@digital.justice.gov.uk")
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getAllUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getAllUsers()

    // Nothing for username2 as there are no differences
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["username1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username1"]?.differences).isEqualTo(
      "not equal: only on right={userName=username1, email=user1auth@digital.justice.gov.uk}"
    )
  }

  @Test
  fun `sync user that is missing from Auth`() {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1@digital.justice.gov.uk")
    )

    val usersFromNomis = listOf(
      NomisUser("username1", "user1@digital.justice.gov.uk"),
      NomisUser("username2", "user2nomis@digital.justice.gov.uk")
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getAllUsers()).thenReturn(usersFromNomis)
    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getAllUsers()

    // Nothing for username1 as there are no differences
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["username2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username2"]?.differences).isEqualTo(
      "not equal: only on left={userName=username2, email=user2nomis@digital.justice.gov.uk}"
    )
  }

  @Test
  fun `sync users with multiple differences`() {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1auth@digital.justice.gov.uk"),
      AuthUser("username2", "user2@digital.justice.gov.uk"),
      AuthUser("username3", "user3auth@digital.justice.gov.uk")
    )

    val usersFromNomis = listOf(
      NomisUser("username2", "user2@digital.justice.gov.uk"),
      NomisUser("username3", "user3@digital.justice.gov.uk")
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getAllUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getAllUsers()

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
  fun `sync user that is has no email in Nomis`() {
    val usersFromAuth = listOf(
      AuthUser("username1", "user1auth@digital.justice.gov.uk"),
      AuthUser("username2", "user2@digital.justice.gov.uk"),
    )

    val usersFromNomis = listOf(
      NomisUser("username1"),
      NomisUser("username2", "user2@digital.justice.gov.uk")
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getAllUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getAllUsers()

    // Nothing for username2 as there are no differences
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["username1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username1"]?.differences).isEqualTo(
      "not equal: value differences={email=(, user1auth@digital.justice.gov.uk)}"
    )
  }

  @Test
  fun `sync user that is has no email in Auth`() {
    val usersFromAuth = listOf(
      AuthUser("username1"),
      AuthUser("username2", "user2@digital.justice.gov.uk"),
    )

    val usersFromNomis = listOf(
      NomisUser("username1", "user1nomis@digital.justice.gov.uk"),
      NomisUser("username2", "user2@digital.justice.gov.uk")
    )
    whenever(authService.getUsers()).thenReturn(usersFromAuth)
    whenever(nomisService.getAllUsers()).thenReturn(usersFromNomis)

    val stats = userSyncService.sync()
    verify(authService).getUsers()
    verify(nomisService).getAllUsers()

    // Nothing for username2 as there are no differences
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["username1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["username1"]?.differences).isEqualTo(
      "not equal: value differences={email=(user1nomis@digital.justice.gov.uk, )}"
    )
  }
}

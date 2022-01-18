package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.google.gson.Gson
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.config.GsonConfig
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_ADM

class RolesSyncServiceTest {
  private val authService: AuthService = mock()
  private val nomisService: NomisApiService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val gson: Gson = GsonConfig().gson()

  private val roleSyncService = RoleSyncService(nomisService, authService, telemetryClient, gson)

  @Test
  fun `sync roles that match`() {
    val role1 = Role(
      "ROLE_1", "Role 1", " description 1",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )
    val role2 = Role(
      "ROLE_2", "Role 2", " description 2",
      listOf(
        AdminTypeReturn("DPS_ADM", "DPS Central Administrator"),
        AdminTypeReturn("DPS_LSA", "DPS Central Administrator")
      )
    )

    val rolesFromAuth = listOf(role1, role2)
    val rolesFromNomis = listOf(
      NomisRole("ROLE_1", "Role 1", true),
      NomisRole("ROLE_2", "Role 2", false),
    )
    whenever(authService.getRoles(anyList())).thenReturn(rolesFromAuth)
    whenever(nomisService.getAllRoles()).thenReturn(rolesFromNomis)

    val statistics = roleSyncService.sync()
    verify(authService).getRoles(listOf(DPS_ADM))
    verify(nomisService).getAllRoles()
    verifyNoMoreInteractions(telemetryClient)
    verifyNoMoreInteractions(nomisService)
    assertThat(statistics.results.size).isEqualTo(0)
  }

  @Test
  fun `sync roles that have same role name truncated string in Nomis - SO VALID`() {
    val role1 = Role(
      "ROLE_1", "Role 1 That is longer than 30 chars", " description 1",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )
    val role2 = Role(
      "ROLE_2", "Role 2", " description 2",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )

    val rolesFromAuth = listOf(role1, role2)
    val rolesFromNomis = listOf(
      NomisRole("ROLE_1", "Role 1 That is longer than 30 ", true),
      NomisRole("ROLE_2", "Role 2", true),
    )
    whenever(authService.getRoles(anyList())).thenReturn(rolesFromAuth)
    whenever(nomisService.getAllRoles()).thenReturn(rolesFromNomis)

    val statistics = roleSyncService.sync()
    verify(authService).getRoles(listOf(DPS_ADM))
    verify(nomisService).getAllRoles()
    verifyNoMoreInteractions(telemetryClient)
    verifyNoMoreInteractions(nomisService)
    assertThat(statistics.results.size).isEqualTo(0)
  }

  @Test
  fun `sync roles that have different role name`() {
    val authRole1 = Role(
      "ROLE_1", "Role 1", " description 1",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )
    val authRole2 = Role(
      "ROLE_2", "Role 2", " description 2",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )

    val rolesFromAuth = listOf(authRole1, authRole2)
    val rolesFromNomis = listOf(
      NomisRole("ROLE_1", "Role 1 Nomis", true),
      NomisRole("ROLE_2", "Role 2", true),
    )
    whenever(authService.getRoles(anyList())).thenReturn(rolesFromAuth)
    whenever(nomisService.getAllRoles()).thenReturn(rolesFromNomis)

    val stats = roleSyncService.sync(false)
    verify(authService).getRoles(listOf(DPS_ADM))
    verify(nomisService).getAllRoles()
    verify(telemetryClient).trackEvent(eq("HMUA-Role-Change"), any(), isNull())
    verify(nomisService).updateRole("ROLE_1", "Role 1", true)

    // Nothing for ROLE_2 as there are no changes
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["ROLE_1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.UPDATE)
    assertThat(stats.results["ROLE_1"]?.differences).isEqualTo("not equal: value differences={roleName=(Role 1 Nomis, Role 1)}")
  }

  @Test
  fun `sync roles that have different role admin types`() {
    val authRole1 = Role(
      "ROLE_1", "Role 1", " description 1",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )
    val authRole2 = Role(
      "ROLE_2", "Role 2", " description 2",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )

    val rolesFromAuth = listOf(authRole1, authRole2)
    val rolesFromNomis = listOf(
      NomisRole("ROLE_1", "Role 1", true),
      NomisRole("ROLE_2", "Role 2", false),
    )
    whenever(authService.getRoles(anyList())).thenReturn(rolesFromAuth)
    whenever(nomisService.getAllRoles()).thenReturn(rolesFromNomis)

    val stats = roleSyncService.sync(false)
    verify(authService).getRoles(listOf(DPS_ADM))
    verify(nomisService).getAllRoles()
    verify(telemetryClient).trackEvent(eq("HMUA-Role-Change"), any(), isNull())
    verify(nomisService).updateRole("ROLE_2", "Role 2", true)

    // Nothing for ROLE_1 as there are no changes
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["ROLE_2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.UPDATE)
    assertThat(stats.results["ROLE_2"]?.differences).isEqualTo("not equal: value differences={adminRoleOnly=(false, true)}")
  }

  @Test
  fun `sync roles that have different role name and admin types`() {
    val authRole1 = Role(
      "ROLE_1", "Role 1", " description 1",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )
    val authRole2 = Role(
      "ROLE_2", "Role 2", " description 2",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )

    val rolesFromAuth = listOf(authRole1, authRole2)
    val rolesFromNomis = listOf(
      NomisRole("ROLE_1", "Role 1", true),
      NomisRole("ROLE_2", "Role 2Nomis", false),
    )
    whenever(authService.getRoles(anyList())).thenReturn(rolesFromAuth)
    whenever(nomisService.getAllRoles()).thenReturn(rolesFromNomis)

    val stats = roleSyncService.sync(false)
    verify(authService).getRoles(listOf(DPS_ADM))
    verify(nomisService).getAllRoles()
    verify(telemetryClient).trackEvent(eq("HMUA-Role-Change"), any(), isNull())
    verify(nomisService).updateRole("ROLE_2", "Role 2", true)

    // Nothing for ROLE_1 as there are no changes
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["ROLE_2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.UPDATE)
    assertThat(stats.results["ROLE_2"]?.differences).isEqualTo("not equal: value differences={roleName=(Role 2Nomis, Role 2), adminRoleOnly=(false, true)}")
  }

  @Test
  fun `sync roles that all have differences`() {
    val authRole1 = Role(
      "ROLE_1", "Role 1", " description 1",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )
    val authRole2 = Role(
      "ROLE_2", "Role 2", " description 2",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )

    val rolesFromAuth = listOf(authRole1, authRole2)
    val rolesFromNomis = listOf(
      NomisRole("ROLE_1", "Role 1Nomis", true),
      NomisRole("ROLE_2", "Role 2Nomis", false),
    )
    whenever(authService.getRoles(anyList())).thenReturn(rolesFromAuth)
    whenever(nomisService.getAllRoles()).thenReturn(rolesFromNomis)

    val stats = roleSyncService.sync(false)
    verify(authService).getRoles(listOf(DPS_ADM))
    verify(nomisService).getAllRoles()
    verify(telemetryClient, times(2)).trackEvent(eq("HMUA-Role-Change"), any(), isNull())
    verify(nomisService).updateRole("ROLE_2", "Role 2", true)

    assertThat(stats.results.size).isEqualTo(2)
    assertThat(stats.results["ROLE_1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.UPDATE)
    assertThat(stats.results["ROLE_1"]?.differences).isEqualTo("not equal: value differences={roleName=(Role 1Nomis, Role 1)}")
    assertThat(stats.results["ROLE_2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.UPDATE)
    assertThat(stats.results["ROLE_2"]?.differences).isEqualTo("not equal: value differences={roleName=(Role 2Nomis, Role 2), adminRoleOnly=(false, true)}")
  }

  @Test
  fun `sync roles that is missing from Nomis`() {
    val authRole1 = Role(
      "ROLE_1", "Role 1", " description 1",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )
    val authRole2 = Role(
      "ROLE_2", "Role 2", " description 2",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )

    val rolesFromAuth = listOf(authRole1, authRole2)
    val rolesFromNomis = listOf(NomisRole("ROLE_2", "Role 2", true))

    whenever(authService.getRoles(anyList())).thenReturn(rolesFromAuth)
    whenever(nomisService.getAllRoles()).thenReturn(rolesFromNomis)

    val stats = roleSyncService.sync(false)
    verify(authService).getRoles(listOf(DPS_ADM))
    verify(nomisService).getAllRoles()
    verify(telemetryClient).trackEvent(eq("HMUA-Role-Change"), any(), isNull())
    verify(nomisService).createRole(NomisRole("ROLE_1", "Role 1", true))

    // Nothing for ROLE_2 as there are no changes
    assertThat(stats.results.size).isEqualTo(1)
    assertThat(stats.results["ROLE_1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.INSERT)
    assertThat(stats.results["ROLE_1"]?.differences).contains("not equal: only on right={roleCode=ROLE_1, roleName=Role 1, adminRoleOnly=true}")
  }

  @Test
  fun `sync role that is missing from Auth`() {
    val authRole1 = Role(
      "ROLE_1", "Role 1", " description 1",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )

    val rolesFromAuth = listOf(authRole1)
    val rolesFromNomis = listOf(
      NomisRole("ROLE_1", "Role 1Nomis", true),
      NomisRole("ROLE_2", "Role 2Nomis", false),
    )
    whenever(authService.getRoles(anyList())).thenReturn(rolesFromAuth)
    whenever(nomisService.getAllRoles()).thenReturn(rolesFromNomis)

    val stats = roleSyncService.sync(false)
    verify(authService).getRoles(listOf(DPS_ADM))
    verify(nomisService).getAllRoles()
    verify(telemetryClient).trackEvent(eq("HMUA-Role-Change"), any(), isNull())
    verify(telemetryClient).trackEvent(eq("HMUA-Role-Change-Failure"), any(), isNull())
    verify(nomisService).updateRole("ROLE_1", "Role 1", true)

    assertThat(stats.results.size).isEqualTo(2)
    assertThat(stats.results["ROLE_1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.UPDATE)
    assertThat(stats.results["ROLE_1"]?.differences).contains("not equal: value differences={roleName=(Role 1Nomis, Role 1)}")
    assertThat(stats.results["ROLE_2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["ROLE_2"]?.differences).contains("not equal: only on left={roleCode=ROLE_2, roleName=Role 2Nomis, adminRoleOnly=false}")
  }

  @Test
  fun `read only sync roles doesn't attempt to save into Nomis`() {
    val authRole1 = Role(
      "ROLE_1", "Role 1", " description 1",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )
    val authRole2 = Role(
      "ROLE_2", "Role 2", " description 2",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )
    val authRole3 = Role(
      "ROLE_3a", "Role 3a", " description 3a",
      listOf(AdminTypeReturn("DPS_ADM", "DPS Central Administrator"))
    )

    val rolesFromAuth = listOf(authRole1, authRole2, authRole3)
    val rolesFromNomis = listOf(
      NomisRole("ROLE_1", "Role 1Nomis", true),
      NomisRole("ROLE_2", "Role 2Nomis", false),
      NomisRole("ROLE_3", "Role 3Nomis", false)
    )
    whenever(authService.getRoles(anyList())).thenReturn(rolesFromAuth)
    whenever(nomisService.getAllRoles()).thenReturn(rolesFromNomis)

    val stats = roleSyncService.sync(true)
    verify(authService).getRoles(listOf(DPS_ADM))
    verify(nomisService).getAllRoles()
    verifyNoMoreInteractions(telemetryClient)
    verifyNoMoreInteractions(nomisService)

    assertThat(stats.results.size).isEqualTo(4)
    assertThat(stats.results["ROLE_1"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["ROLE_1"]?.differences).isEqualTo("not equal: value differences={roleName=(Role 1Nomis, Role 1)}")
    assertThat(stats.results["ROLE_2"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["ROLE_2"]?.differences).isEqualTo("not equal: value differences={roleName=(Role 2Nomis, Role 2), adminRoleOnly=(false, true)}")
    assertThat(stats.results["ROLE_3"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["ROLE_3"]?.differences).isEqualTo("not equal: only on left={roleCode=ROLE_3, roleName=Role 3Nomis, adminRoleOnly=false}")
    assertThat(stats.results["ROLE_3a"]?.updateType).isEqualTo(SyncDifferences.UpdateType.NONE)
    assertThat(stats.results["ROLE_3a"]?.differences).isEqualTo("not equal: only on right={roleCode=ROLE_3a, roleName=Role 3a, adminRoleOnly=true}")
  }
}

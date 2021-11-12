package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.service.AdminType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleDifferences.UpdateType.ERROR
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleDifferences.UpdateType.INSERT
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleDifferences.UpdateType.NONE
import uk.gov.justice.digital.hmpps.manageusersapi.service.RoleDifferences.UpdateType.UPDATE
import java.lang.reflect.Type

@Service
class RoleSyncService(
  private val nomisApiService: NomisApiService,
  private val authService: AuthService,
  private val telemetryClient: TelemetryClient,
  private val gson: Gson
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sync(): SyncStatistics {
    return syncAllRoles(authService.getRoles(listOf(DPS_ADM)), nomisApiService.getAllRoles())
  }

  private fun syncAllRoles(
    rolesFromAuth: List<Role>,
    rolesFromNomis: List<NomisRole>
  ): SyncStatistics {

    val rolesFromNomisMap = rolesFromNomis.map { RoleDataToSync(it) }.associateBy { it.roleCode }
    val rolesFromAuthMap = rolesFromAuth.map { RoleDataToSync(it) }.associateBy { it.roleCode }

    val stats = SyncStatistics()
    rolesFromAuthMap.filterMatching(rolesFromNomisMap).forEach {
      syncRole(rolesFromNomisMap[it.key], it.value, stats)
    }
    rolesFromAuthMap.filterNew(rolesFromNomisMap).forEach {
      syncRole(null, it.value, stats)
    }

    rolesFromNomisMap.filterMissing(rolesFromAuthMap).forEach {
      syncRole(rolesFromNomisMap[it.key], it.value, stats)
    }
    return stats
  }

  private fun <K, V> Map<out K, V>.filterNew(rolesMap: Map<K, V>): Map<K, V> = filter { r -> rolesMap[r.key] == null }
  private fun <K, V> Map<out K, V>.filterMatching(rolesMap: Map<K, V>): Map<K, V> =
    filter { r -> rolesMap[r.key] != null }

  private fun <K, V> Map<out K, RoleDataToSync>.filterMissing(rolesMap: Map<K, V>): Map<K, RoleDataToSync> {
    return filter { r -> rolesMap[r.key] == null }
  }

  fun syncRole(currentRoleData: RoleDataToSync?, newRoleData: RoleDataToSync, stats: SyncStatistics) {

    val diff = checkForDifferences(currentRoleData, newRoleData)
    if (!diff.areEqual()) {
      stats.roles[newRoleData.roleCode] = RoleDifferences(newRoleData.roleCode, diff.toString())
      try {
        storeInNomis(currentRoleData, newRoleData, stats)
        if (stats.roles[newRoleData.roleCode]?.updateType != NONE) {
          val trackingAttributes = mapOf(
            "roleCode" to newRoleData.roleCode,
            "differences" to stats.roles[newRoleData.roleCode]?.differences,
          )
          telemetryClient.trackEvent("HMUA-Role-Change", trackingAttributes, null)
        }
      } catch (e: Exception) {
        stats.roles[newRoleData.roleCode] = stats.roles[newRoleData.roleCode]!!.copy(updateType = ERROR)

        log.error("Failed to update {} - message = {}", newRoleData.roleCode, e.message)
        telemetryClient.trackEvent("HMUA-Role-Change-Failure", mapOf("roleCode" to newRoleData.roleCode), null)
      }
    }
  }

  private fun storeInNomis(
    currentRoleData: RoleDataToSync?,
    newRoleData: RoleDataToSync,
    stats: SyncStatistics
  ) {
    currentRoleData?.let {
      if (newRoleData != currentRoleData) { // don't update if equal
        nomisApiService.updateRole(it.roleCode, newRoleData.roleName, newRoleData.adminRoleOnly)
        stats.roles[newRoleData.roleCode] = stats.roles[newRoleData.roleCode]!!.copy(updateType = UPDATE)
      }
    } ?: run {
      // missing from Nomis
      nomisApiService.createRole(NomisRole(newRoleData.roleCode, newRoleData.roleName, newRoleData.adminRoleOnly))
      stats.roles[newRoleData.roleCode] = stats.roles[newRoleData.roleCode]!!.copy(updateType = INSERT)
    }

    if (stats.roles[newRoleData.roleCode]?.updateType == NONE) {
      stats.roles.remove(newRoleData.roleCode)
    }
  }

  fun checkForDifferences(
    existingRecord: RoleDataToSync?,
    newRecord: RoleDataToSync
  ): MapDifference<String, Any> {
    val type: Type = object : TypeToken<Map<String, Any>>() {}.type
    val leftMap: Map<String, Any> =
      if (existingRecord != null) {
        gson.fromJson(
          gson.toJson(existingRecord), type
        )
      } else mapOf()
    val rightMap: Map<String, Any> = gson.fromJson(gson.toJson(newRecord), type)
    return Maps.difference(leftMap, rightMap)
  }
}

data class NomisRole(
  @Schema(description = "Role Code", example = "GLOBAL_SEARCH", required = true)
  val code: String,

  @Schema(description = "Role Name", example = "Global Search Role", required = true)
  val name: String,

  @Schema(description = "If the role is for admin users only", example = "false", required = true)
  val adminRoleOnly: Boolean
)

data class RoleDataToSync(
  val roleCode: String,
  val roleName: String,
  val adminRoleOnly: Boolean
) {
  constructor(roleFromAuth: Role) :
    this(
      roleFromAuth.roleCode,
      roleFromAuth.roleName.take(30),
      DPS_LSA !in roleFromAuth.adminType.asAdminTypes()
    )

  constructor(roleFromNomis: NomisRole) :
    this(
      roleFromNomis.code,
      roleFromNomis.name,
      roleFromNomis.adminRoleOnly
    )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sync Statistics")
data class SyncStatistics(
  @Schema(description = "Map of all roles have have been inserted, updated or errored")
  val roles: MutableMap<String, RoleDifferences> = mutableMapOf()
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Role Changes")
data class RoleDifferences(
  @Schema(description = "The code of the Role", example = "GLOBAL_SEARCH") val roleCode: String,
  @Schema(description = "Differences listed", example = "Global Searcher") val differences: String,
  @Schema(description = "Type of update", example = "INSERT") val updateType: UpdateType = NONE
) {
  enum class UpdateType {
    NONE, INSERT, UPDATE, ERROR
  }
}

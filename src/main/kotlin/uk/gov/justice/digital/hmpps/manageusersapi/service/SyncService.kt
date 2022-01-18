package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import io.swagger.v3.oas.annotations.media.Schema

abstract class SyncService(
  private val gson: Gson
) {
  fun <K, V> Map<out K, V>.filterNew(syncMap: Map<K, V>): Map<K, V> = filter { r -> syncMap[r.key] == null }

  fun <K, V> Map<out K, V>.filterMatching(syncMap: Map<K, V>): Map<K, V> =
    filter { r -> syncMap[r.key] != null }

  fun <K, V, T> Map<out K, T>.filterMissing(syncMap: Map<K, V>): Map<K, T> {
    return filter { r -> syncMap[r.key] == null }
  }

  fun <T> checkForDifferences(
    leftRecord: T?,
    rightRecord: T?
  ): MapDifference<String, Any> {
    val leftMap = leftRecord?.let { it.asMap() } ?: mapOf()
    val rightMap = rightRecord?.let { it.asMap() } ?: mapOf()
    return Maps.difference(leftMap, rightMap)
  }
  private fun <T> T.asMap(): Map<String, Any> {
    return (
      gson.fromJson(
        gson.toJson(this), object : TypeToken<Map<String, Any>>() {}.type
      )
      )
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sync Statistics")
data class SyncStatistics(
  @Schema(description = "Map of all items have have been inserted, updated or errored")
  val results: MutableMap<String, SyncDifferences> = mutableMapOf()
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Sync Differences")
data class SyncDifferences(
  @Schema(description = "The id of the Object to sync", example = "GLOBAL_SEARCH") val id: String,
  @Schema(description = "Differences listed", example = "Global Searcher") val differences: String,
  @Schema(description = "Type of update", example = "INSERT") val updateType: UpdateType = UpdateType.NONE
) {
  enum class UpdateType {
    NONE, INSERT, UPDATE, ERROR
  }
}

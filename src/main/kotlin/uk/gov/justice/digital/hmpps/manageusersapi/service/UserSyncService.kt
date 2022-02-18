package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.google.common.collect.MapDifference
import com.google.gson.Gson
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserSyncService(
  private val nomisApiService: NomisApiService,
  private val authService: AuthService,
  gson: Gson
) : SyncService(gson) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sync(caseSensitive: Boolean = true, usePrimaryEmail: Boolean = false): SyncStatistics {
    return syncAllData(authService.getUsers(), nomisApiService.findAllActiveUsers(), caseSensitive, usePrimaryEmail)
  }

  private fun syncAllData(
    usersFromAuth: List<AuthUser>,
    usersFromNomis: List<NomisUser>,
    caseSensitive: Boolean,
    usePrimaryEmail: Boolean = false
  ): SyncStatistics {
    log.debug("Syncing ${usersFromAuth.size} auth (nomis) users against ${usersFromNomis.size} nomis users")

    val usersFromAuthMap = usersFromAuth.map { UserDataToSync(it.username, it.email) }.associateBy { it.userName }
    val usersFromNomisMap = usersFromNomis.map { it ->
      UserDataToSync(
        it.username,
        it.email?.let
        {
          val emailToUse = if (usePrimaryEmail) {
            it.split(",").primaryEmail()
          } else it
          if (caseSensitive) emailToUse else emailToUse?.lowercase()
        }
      )
    }.associateBy { it.userName }

    val stats = SyncStatistics()
    usersFromAuthMap.filterMatching(usersFromNomisMap).forEach {
      syncUser(usersFromNomisMap[it.key], it.value, stats)
    }
    usersFromAuthMap.filterNew(usersFromNomisMap).forEach {
      syncUser(null, it.value, stats)
    }
    // We are not interested in those users in Nomis but not in Auth
    log.debug("Total sync differences = ${stats.results.size} with caseSensitive set to $caseSensitive")
    return stats
  }

  fun syncUser(currentUserData: UserDataToSync?, newUserData: UserDataToSync?, stats: SyncStatistics) {

    val diff = checkForDifferences(currentUserData, newUserData)
    if (!diff.areEqual()) {
      diff.storeStatistics(currentUserData, newUserData, stats)
    }
  }

  private fun MapDifference<String, Any>.storeStatistics(
    currentUserData: UserDataToSync?,
    newUserData: UserDataToSync?,
    stats: SyncStatistics
  ) {
    if (entriesOnlyOnLeft().isNotEmpty()) {
      stats.results[currentUserData!!.userName] = SyncDifferences(currentUserData.userName, toString())
    } else
      stats.results[newUserData!!.userName] = SyncDifferences(newUserData.userName, toString())
  }
}

fun List<String>.primaryEmail(): String =
  (firstOrNull { e -> e.contains("justice.gov.uk") } ?: run { first() }).trim()

data class AuthUser(
  @Schema(description = "User Name in Auth", example = "Global Search User", required = true)
  val username: String,

  @Schema(description = "email", example = "jimauth@justice.gov.uk", required = false)
  val email: String? = null
)

data class NomisUser(
  @Schema(description = "User Name in Nomis", example = "Global Search User", required = true)
  val username: String,

  @Schema(description = "primaryEmail", example = "jimnomis@justice.gov.uk", required = false)
  val email: String? = null
)

data class UserDataToSync(
  val userName: String,
  val email: String?
)

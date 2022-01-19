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
  private val gson: Gson
) : SyncService(gson) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sync(): SyncStatistics {
    return syncAllData(authService.getUsers(), nomisApiService.getAllUsers())
  }

  private fun syncAllData(
    usersFromAuth: List<AuthUser>,
    usersFromNomis: List<NomisUser>,
  ): SyncStatistics {

    val usersFromAuthMap = usersFromAuth.map { UserDataToSync(it.username, it.email) }.associateBy { it.userName }
    val usersFromNomisMap = usersFromNomis.map { UserDataToSync(it.username, it.email) }.associateBy { it.userName }

    val stats = SyncStatistics()
    usersFromAuthMap.filterMatching(usersFromNomisMap).forEach {
      syncUser(usersFromNomisMap[it.key], it.value, stats)
    }
    usersFromAuthMap.filterNew(usersFromNomisMap).forEach {
      syncUser(null, it.value, stats)
    }
    usersFromNomisMap.filterMissing(usersFromAuthMap).forEach {
      syncUser(usersFromNomisMap[it.key], null, stats)
    }
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

data class AuthUser(
  @Schema(description = "User Name in Auth", example = "Global Search User", required = true)
  val username: String,

  @Schema(description = "email", example = "jimauth@justice.gov.uk", required = true)
  val email: String?
)

data class NomisUser(
  @Schema(description = "User Name in Nomis", example = "Global Search User", required = true)
  val username: String,

  @Schema(description = "email", example = "jimnomis@justice.gov.uk", required = true)
  val email: String?
)

data class UserDataToSync(
  val userName: String,
  val email: String?
)
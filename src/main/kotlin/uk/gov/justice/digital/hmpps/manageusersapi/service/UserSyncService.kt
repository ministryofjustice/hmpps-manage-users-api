package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.google.common.collect.MapDifference
import com.google.gson.Gson
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service
class UserSyncService(
  private val nomisApiService: NomisApiService,
  private val authService: AuthService,
  gson: Gson
) : SyncService(gson) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sync(): SyncStatistics {
    val authUsers = authService.getUsers()
    log.debug("Fetched ${authUsers.size} nomis users auth users ")
    val nomisUsers = mutableListOf<NomisUser>()
    authUsers.forEach {
      try {
        nomisUsers.add(nomisApiService.getUser(it.username))
      } catch (e: WebClientResponseException) {
        // if the username doesn't exist in name, we don't care - the sync process will take care of this
        if (!e.statusCode.equals(HttpStatus.NOT_FOUND)) throw e
      }
    }
    log.debug("Fetched ${nomisUsers.size} users from nomis ")
    return syncAllData(authUsers, nomisUsers)
  }

  private fun syncAllData(
    usersFromAuth: List<AuthUser>,
    usersFromNomis: List<NomisUser>
  ): SyncStatistics {

    val usersFromAuthMap = usersFromAuth.map { UserDataToSync(it.username, it.email) }.associateBy { it.userName }
    val usersFromNomisMap = usersFromNomis.map { UserDataToSync(it.username, it.primaryEmail) }.associateBy { it.userName }

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

  @Schema(description = "email", example = "jimauth@justice.gov.uk", required = false)
  val email: String? = null
)

data class NomisUser(
  @Schema(description = "User Name in Nomis", example = "Global Search User", required = true)
  val username: String,

  @Schema(description = "primaryEmail", example = "jimnomis@justice.gov.uk", required = false)
  val primaryEmail: String? = null
)

data class UserDataToSync(
  val userName: String,
  val email: String?
)

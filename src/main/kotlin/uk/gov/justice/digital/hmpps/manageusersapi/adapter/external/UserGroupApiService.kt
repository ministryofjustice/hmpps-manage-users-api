package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserGroup
import java.util.UUID

@Service
class UserGroupApiService(
  @Qualifier("externalUsersUserWebClientUtils") val userWebClientUtils: WebClientUtils,
  @Qualifier("externalUsersWebClientUtils") val serviceWebClientUtils: WebClientUtils,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun deleteGroupByUserId(userId: UUID, group: String) {
    log.debug("Delete group {} for user {}", group, userId)
    userWebClientUtils.delete("/users/{userId}/groups/{group}", userId, group)
  }

  fun addGroupByUserId(userId: UUID, group: String) {
    log.debug("Adding group {} for user {}", group, userId)
    userWebClientUtils.put("/users/{userId}/groups/{group}", userId, group)
  }

  fun getUserGroups(userId: UUID, children: Boolean): List<UserGroup> = serviceWebClientUtils.getWithParams("/users/$userId/groups", GroupList::class.java, mapOf("children" to children))

  fun getMyAssignableGroups(): List<UserGroup> = userWebClientUtils.get("/users/me/assignable-groups", GroupList::class.java)
}

class GroupList : MutableList<UserGroup> by ArrayList()

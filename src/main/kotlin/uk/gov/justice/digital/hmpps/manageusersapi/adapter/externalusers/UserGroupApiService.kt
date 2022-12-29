package uk.gov.justice.digital.hmpps.manageusersapi.adapter.externalusers

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserGroup
import java.util.UUID

@Service
class UserGroupApiService(
  @Qualifier("externalUsersWebClientUtils") val externalUsersWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun deleteGroupByUserId(userId: UUID, group: String) {
    log.debug("Delete group {} for user {}", group, userId)
    externalUsersWebClientUtils.delete("/users/$userId/groups/$group")
  }

  fun addGroupByUserId(userId: UUID, group: String) {
    log.debug("Adding group {} for user {}", group, userId)
    externalUsersWebClientUtils.put("/users/$userId/groups/$group")
  }

  fun getUserGroups(userId: UUID, children: Boolean): List<UserGroup> =
    externalUsersWebClientUtils.getWithParams("/users/$userId/groups", GroupList::class.java, mapOf("children" to children))

  fun getMyAssignableGroups(): List<UserGroup> =
    externalUsersWebClientUtils.get("/users/me/assignable-groups", GroupList::class.java)
}

class GroupList : MutableList<UserGroup> by ArrayList()

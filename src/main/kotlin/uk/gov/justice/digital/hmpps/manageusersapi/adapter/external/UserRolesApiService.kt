package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.UserRole
import java.util.UUID

@Service
class UserRolesApiService(
  @Qualifier("externalUsersWebClientUtils") val externalUsersWebClientUtils: WebClientUtils
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getUserRoles(userId: UUID): List<UserRole> =
    externalUsersWebClientUtils.get("/users/$userId/roles", UserRoleList::class.java)

  fun addRolesByUserId(userId: UUID, roleCodes: List<String>) {
    log.debug("Adding roles {} for user {}", roleCodes, userId)
    externalUsersWebClientUtils.post("/users/$userId/roles", roleCodes)
  }

  fun deleteRoleByUserId(userId: UUID, role: String) {
    log.debug("Delete role {} for user {}", role, userId)
    externalUsersWebClientUtils.delete("/users/$userId/roles/$role")
  }

  fun getAssignableRoles(userId: UUID) =
    externalUsersWebClientUtils.get("/users/$userId/assignable-roles", UserRoleList::class.java)
}

class UserRoleList : MutableList<UserRole> by ArrayList()

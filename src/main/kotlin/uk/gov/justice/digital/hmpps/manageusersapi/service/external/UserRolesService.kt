package uk.gov.justice.digital.hmpps.manageusersapi.service.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.external.UserRolesApiService
import java.util.UUID

@Service("ExternalUserRolesService")
class UserRolesService(
  val userRolesApiService: UserRolesApiService
) {
  fun getUserRoles(userId: UUID) = userRolesApiService.getUserRoles(userId)
  fun addRolesByUserId(userId: UUID, roleCodes: List<String>) = userRolesApiService.addRolesByUserId(userId, roleCodes)
  fun removeRoleByUserId(userId: UUID, role: String) = userRolesApiService.deleteRoleByUserId(userId, role)
  fun getAssignableRoles(userId: UUID) = userRolesApiService.getAssignableRoles(userId)
}

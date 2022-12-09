package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ExternalUserRolesService(
  val externalUsersApiService: ExternalUsersApiService
) {
  fun getUserRoles(userId: UUID) = externalUsersApiService.getUserRoles(userId)
  fun addRolesByUserId(userId: UUID, roleCodes: List<String>) = externalUsersApiService.addRolesByUserId(userId, roleCodes)
  fun removeRoleByUserId(userId: UUID, role: String) = externalUsersApiService.deleteRoleByUserId(userId, role)
  fun getAssignableRoles(userId: UUID) = externalUsersApiService.getAssignableRoles(userId)
}

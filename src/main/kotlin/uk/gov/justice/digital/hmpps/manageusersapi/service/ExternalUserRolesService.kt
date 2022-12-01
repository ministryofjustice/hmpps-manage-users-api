package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ExternalUserRolesService(
  val externalUsersApiService: ExternalUsersApiService,
) {
  fun getUserRoles(userId: UUID) = externalUsersApiService.getUserRoles(userId)
  fun removeRoleByUserId(userId: UUID, role: String) = externalUsersApiService.deleteRoleByUserId(userId, role)
}

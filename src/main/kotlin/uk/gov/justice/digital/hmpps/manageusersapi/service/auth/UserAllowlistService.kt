package uk.gov.justice.digital.hmpps.manageusersapi.service.auth

import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserAllowlistAddRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserAllowlistDetail
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserAllowlistPatchRequest
import java.util.*

@Service("UserAllowListService")
class UserAllowlistService(
  val authApiService: AuthApiService,
) {
  fun addUser(addUserRequest: UserAllowlistAddRequest) = authApiService.addUserToAllowlist(addUserRequest)

  fun getAllUsers(name: String?, status: Status, pageable: Pageable): PagedResponse<UserAllowlistDetail> = authApiService.getAllAllowlistUsers(name, status, pageable)

  fun updateUserAccess(id: UUID, updateUserAccessRequest: UserAllowlistPatchRequest) = authApiService.updateAllowlistUserAccess(id, updateUserAccessRequest)

  fun getUser(username: String): UserAllowlistDetail = authApiService.getAllowlistUser(username)
}

enum class Status {
  ACTIVE,
  EXPIRED,
  ALL,
}

package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource

@Service("UserSearchService")
class UserSearchService(
  val authApiService: AuthApiService,
) {
  fun searchUsers(
    name: String?,
    status: Status?,
    authSources: List<AuthSource>?,
    page: Int?,
    size: Int?,
    sort: String?,
  ) = authApiService.findUsers(
    name,
    status,
    authSources,
    page,
    size,
    sort,
  )
}

enum class Status {
  ACTIVE, INACTIVE, ALL
}

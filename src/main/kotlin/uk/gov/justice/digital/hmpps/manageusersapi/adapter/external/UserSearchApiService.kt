package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.ExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.Status
import java.util.UUID

@Service
class UserSearchApiService(
  @Qualifier("externalUsersWebClientUtils") val serviceWebClientUtils: WebClientUtils,
  @Qualifier("externalUsersUserWebClientUtils") val userWebClientUtils: WebClientUtils,
) {
  fun findUsersByEmail(email: String): List<ExternalUser>? =
    userWebClientUtils.getIfPresent("/users?email=$email", UserList::class.java)

  fun findUserByUsername(username: String): ExternalUser =
    userWebClientUtils.get("/users/$username", ExternalUser::class.java)

  fun findUserByUsernameIfPresent(username: String): ExternalUser? =
    userWebClientUtils.getIfPresent("/users/$username", ExternalUser::class.java)

  fun findByUserId(userId: UUID): ExternalUser =
    userWebClientUtils.get("/users/id/$userId", ExternalUser::class.java)

  fun findUserByUsernameOrNull(username: String): ExternalUser? =
    serviceWebClientUtils.getIgnoreError("/users/$username", ExternalUser::class.java)

  fun findUsers(
    name: String?,
    roles: List<String>?,
    groups: List<String>?,
    pageable: Pageable,
    status: Status,
  ) =
    userWebClientUtils.getWithParams(
      "/users/search",
      object : ParameterizedTypeReference<PagedResponse<ExternalUserDetailsDto>>() {},
      mapOf(
        "name" to name,
        "roles" to roles?.joinToString(","),
        "groups" to groups?.joinToString(","),
        "status" to status,
        "page" to pageable.pageNumber,
        "size" to pageable.pageSize,
      ),
    )
}

class UserList : MutableList<ExternalUser> by ArrayList()

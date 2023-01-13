package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.ExternalUserDetailsDto
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.Status
import java.util.UUID
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.ExternalUserDetailsForEmailUpdateDto

@Service
class UserSearchApiService(
  @Qualifier("externalUsersWebClientUtils") val externalUsersWebClientUtils: WebClientUtils
) {
  fun findUsersByEmail(email: String): List<ExternalUserDetailsDto>? =
    externalUsersWebClientUtils.getIfPresent("/users?email=$email", UserList::class.java)

  fun findUserByUsername(userName: String): ExternalUserDetailsDto? =
    externalUsersWebClientUtils.getIfPresent("/users/$userName", ExternalUserDetailsDto::class.java)

  fun findUserDetailsByUserIdForEmailUpdate(uuid: UUID): ExternalUserDetailsForEmailUpdateDto =
    externalUsersWebClientUtils.get("/users/userid/$uuid", ExternalUserDetailsForEmailUpdateDto::class.java)

  fun findUsers(
    name: String?,
    roles: List<String>?,
    groups: List<String>?,
    pageable: Pageable,
    status: Status
  ) =
    externalUsersWebClientUtils.getWithParams(
      "/users/search", object : ParameterizedTypeReference<PagedResponse<ExternalUserDetailsDto>>() {},
      mapOf(
        "name" to name,
        "roles" to roles?.joinToString(","),
        "groups" to groups?.joinToString(","),
        "status" to status,
        "page" to pageable.pageNumber,
        "size" to pageable.pageSize
      )
    )
}

class UserList : MutableList<ExternalUserDetailsDto> by ArrayList()
package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role
import uk.gov.justice.digital.hmpps.manageusersapi.resource.RolesPaged
import java.time.Duration

@Service
class ExternalUsersApiService(
  @Qualifier("externalUsersWebClient") val externalUsersWebClient: WebClient,
  @Value("\${api.timeout:10s}")
  val timeout: Duration
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getRoles(adminTypes: List<AdminType>?): List<Role> {
    return externalUsersWebClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/roles")
          .queryParam("adminTypes", adminTypes)
          .build()
      }
      .retrieve()
      .bodyToMono(RoleList::class.java)
      .block()!!
  }

  fun getPagedRoles(
    page: Int,
    size: Int,
    sort: String,
    roleName: String?,
    roleCode: String?,
    adminTypes: List<AdminType>?
  ): RolesPaged {

    return externalUsersWebClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/roles/paged")
          .queryParam("page", page)
          .queryParam("size", size)
          .queryParam("sort", sort)
          .queryParam("roleName", roleName)
          .queryParam("roleCode", roleCode)
          .queryParam("adminTypes", adminTypes)
          .build()
      }
      .retrieve()
      .bodyToMono(RolesPaged::class.java)
      .block()!!
  }

  @Throws(RoleNotFoundException::class)
  fun getRoleDetail(roleCode: String): Role {
    return externalUsersWebClient.get()
      .uri("/roles/$roleCode")
      .retrieve()
      .bodyToMono(Role::class.java)
      .block(timeout) ?: throw RoleNotFoundException("get", roleCode, "notfound")
  }
}

class RoleList : MutableList<Role> by ArrayList()

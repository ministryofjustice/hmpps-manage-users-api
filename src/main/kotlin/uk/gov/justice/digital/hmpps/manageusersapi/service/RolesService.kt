package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.resource.Role

@Service
class RolesService(
  val authWebClient: WebClient,
) {

  @Throws(RoleNotFoundException::class)
  fun getRoleDetail(roleCode: String): Role {
    try {
      return authWebClient.get()
        .uri("/api/roles/$roleCode")
        .retrieve()
        .bodyToMono(Role::class.java)
        .block() ?: throw RoleNotFoundException("get", roleCode, "notfound")
    } catch (e: WebClientResponseException) {
      throw if (e.statusCode.equals(HttpStatus.NOT_FOUND)) RoleNotFoundException("get", roleCode, "notfound") else e
    }
  }
}

class RoleNotFoundException(action: String, role: String, errorCode: String) :
  Exception("Unable to $action role: $role with reason: $errorCode")

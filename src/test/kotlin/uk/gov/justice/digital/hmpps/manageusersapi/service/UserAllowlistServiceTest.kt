package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.AccessPeriod
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserAllowlistAddRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserAllowlistPatchRequest
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.Status
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.UserAllowlistService
import java.util.*

class UserAllowlistServiceTest {
  private val authApiService: AuthApiService = mock()
  private val userAllowlistService = UserAllowlistService(authApiService)

  @Test
  fun `adding an allow list user calls the auth API`() {
    val allowListAddRequest = UserAllowlistAddRequest(
      "LAMISHACT",
      "consuella.tapscott@justice.gov.uk",
      "Consuella",
      "Tapscott",
      "testing",
      AccessPeriod.ONE_MONTH,
    )

    userAllowlistService.addUser(allowListAddRequest)
    verify(authApiService).addUserToAllowlist(allowListAddRequest)
  }

  @Test
  fun `updating an allow list user's access calls the auth API`() {
    val id = UUID.fromString("414fa9cf-b4c4-48a2-a291-d45d4e07194f")
    val allowListUpdateRequest = UserAllowlistPatchRequest(
      "testing",
      AccessPeriod.ONE_MONTH,
    )

    userAllowlistService.updateUserAccess(id, allowListUpdateRequest)
    verify(authApiService).updateAllowlistUserAccess(id, allowListUpdateRequest)
  }

  @Test
  fun `getting an allow list user calls the auth API`() {
    val username = "AUTH_ADM"

    userAllowlistService.getUser(username)
    verify(authApiService).getAllowlistUser(username)
  }

  @Test
  fun `getting all allow list users calls the auth API`() {
    val name = null
    val status = Status.ALL
    val pageable = Pageable.unpaged()

    userAllowlistService.getAllUsers(name, status, pageable)
    verify(authApiService).getAllAllowlistUsers(name, status, pageable)
  }
}

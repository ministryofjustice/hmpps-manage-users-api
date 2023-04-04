package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.prison.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.NotFoundException

@Service
class SyncService(
  private val authApiService: AuthApiService,
  private val prisonUserApiService: UserApiService,
) {
  fun syncEmailWithNomis(username: String) =
    prisonUserApiService.findUserByUsername(username)
      ?.let { if (it.email != null) authApiService.syncEmailWithNomis(username, it.email) }
      ?: throw NotFoundException("Account for username $username not found")
}

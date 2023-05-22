package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserDetails
import uk.gov.justice.digital.hmpps.manageusersapi.model.PrisonUser
import uk.gov.justice.digital.hmpps.manageusersapi.service.auth.NotFoundException

class SyncServiceTest {
  private val prisonUserApiService: UserApiService = mock()
  private val authApiService: AuthApiService = mock()
  private val syncService = SyncService(authApiService, prisonUserApiService)

  @Nested
  inner class SyncEmail {

    @Test
    fun `no match in Nomis`() {
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(null)

      assertThatThrownBy { syncService.syncEmailWithNomis("nomis_user") }
        .isInstanceOf(NotFoundException::class.java)
        .hasMessage("Account for username nomis_user not found")
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `don't sync email if null`() {
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(
        PrisonUser(
          "NUSER_GEN",
          "Nomis",
          123456,
          "Take",
          "MDI",
          null,
        ),
      )

      syncService.syncEmailWithNomis("nomis_user")
      verify(prisonUserApiService).findUserByUsername("nomis_user")
      verifyNoInteractions(authApiService)
    }

    @Test
    fun `sync email`() {
      whenever(prisonUserApiService.findUserByUsername(anyString())).thenReturn(createPrisonUserDetails())

      syncService.syncEmailWithNomis("nomis_user")
      verify(prisonUserApiService).findUserByUsername("nomis_user")
      verify(authApiService).syncEmailWithNomis("nomis_user", "nomis.usergen@digital.justice.gov.uk")
    }
  }
}

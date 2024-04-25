package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.*
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.CaseloadsApiService
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserCaseloadDetails

class UserCaseloadsServiceTest {
  private val prisonCaseloadsApiService: CaseloadsApiService   = mock()
   private val userCaseloadsService = UserCaseloadService(prisonCaseloadsApiService)

  @Test
  fun `gets user caseloads`() {
    val userCaseloads = createPrisonUserCaseloadDetails()
    whenever(prisonCaseloadsApiService.getUserCaseloads(anyString())).thenReturn(userCaseloads)

    val caseloads = userCaseloadsService.getUserCaseloads("NUSER_GEN")

    verify(prisonCaseloadsApiService).getUserCaseloads("NUSER_GEN")
    assertThat(caseloads).isEqualTo(userCaseloads)
  }

  @Test
  fun `adds user caseloads`() {
    val userCaseloads = createPrisonUserCaseloadDetails()
    whenever(prisonCaseloadsApiService.addUserCaseloads(anyString(), any())).thenReturn(userCaseloads)

    val caseloads = userCaseloadsService.addUserCaseloads("NUSER_GEN", listOf("BXI", "WLI"))

    verify(prisonCaseloadsApiService).addUserCaseloads("NUSER_GEN", listOf("BXI", "WLI"))
    assertThat(caseloads).isEqualTo(userCaseloads)
  }

  @Test
  fun `remove user caseload`() {
    val userCaseloads = createPrisonUserCaseloadDetails()
    whenever(prisonCaseloadsApiService.removeCaseloadFromUser(anyString(), anyString())).thenReturn(userCaseloads)

    val caseloads = userCaseloadsService.removeCaseloadFromUser("NUSER_GEN", "LEI")

    verify(prisonCaseloadsApiService).removeCaseloadFromUser("NUSER_GEN", "LEI")
    assertThat(caseloads).isEqualTo(userCaseloads)
  }

}

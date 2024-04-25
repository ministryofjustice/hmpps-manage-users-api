package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.*
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserCaseloadService
import uk.gov.justice.digital.hmpps.manageusersapi.fixtures.UserFixture.Companion.createPrisonUserCaseloadDetails

class UserCaseloadsControllerTest {
  private val userCaseloadsService: UserCaseloadService = mock()
  private val userCaseloadsController = UserCaseloadsController(userCaseloadsService)

  @Test
  fun `get user roles`() {
    val details = createPrisonUserCaseloadDetails()
    whenever(userCaseloadsService.getUserCaseloads("SOME_USER")).thenReturn(details)

    val userCaseloads = userCaseloadsController.getUserCaseloads("SOME_USER").body

    verify(userCaseloadsService).getUserCaseloads("SOME_USER")
    assertThat(userCaseloads).isEqualTo(details)
  }

  @Test
  fun `add user caseloads`() {
    val details = createPrisonUserCaseloadDetails()
    whenever(userCaseloadsService.addUserCaseloads(anyString(), any())).thenReturn(details)

    val userCaseloads = userCaseloadsController.addCaseloads("SOME_USER", listOf("LEI", "MDI"))

    verify(userCaseloadsService).addUserCaseloads("SOME_USER", listOf("LEI", "MDI"))
    assertThat(userCaseloads).isEqualTo(details)
  }

  @Test
  fun `remove user caseloads`() {
    val details = createPrisonUserCaseloadDetails()
    whenever(userCaseloadsService.removeCaseloadFromUser(anyString(), anyString())).thenReturn(details)

    val userCaseloads = userCaseloadsController.removeCaseload("SOME_USER", "BXI")

    verify(userCaseloadsService).removeCaseloadFromUser("SOME_USER", "BXI")
    assertThat(userCaseloads).isEqualTo(details)
  }

}

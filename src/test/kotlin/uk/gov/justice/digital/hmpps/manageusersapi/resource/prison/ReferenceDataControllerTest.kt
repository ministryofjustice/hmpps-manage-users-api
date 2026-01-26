package uk.gov.justice.digital.hmpps.manageusersapi.resource.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.CaseloadList
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.ReferenceDataService

class ReferenceDataControllerTest {

  private val referenceDataService = mock<ReferenceDataService>()
  private val referenceDataController = ReferenceDataController(referenceDataService)

  @Test
  fun `get caseloads`() {
    whenever(referenceDataService.getCaseloads()).thenReturn(
      CaseloadList().apply {
        add(PrisonCaseload("TESTING-1234", "TEST-CASELOAD-1", "GENERAL"))
        add(PrisonCaseload("TESTING-1235", "TEST-CASELOAD-2", "GENERAL"))
      },
    )

    val caseloads = referenceDataController.getCaseload()
    verify(referenceDataService).getCaseloads()
    assertThat(caseloads).containsExactlyInAnyOrder(
      PrisonCaseload("TESTING-1234", "TEST-CASELOAD-1", "GENERAL"),
      PrisonCaseload("TESTING-1235", "TEST-CASELOAD-2", "GENERAL"),
    )
  }
}

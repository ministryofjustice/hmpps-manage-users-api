package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.ReferenceDataApiService

@Service("ReferenceDataService")
class ReferenceDataService(
  private val prisonReferenceDataService: ReferenceDataApiService,
) {
  fun getCaseloads() = prisonReferenceDataService.getCaseloads()
}

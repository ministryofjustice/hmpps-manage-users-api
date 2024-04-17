package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.*

@Service(value = "nomisReferenceDataService")
class ReferenceDataApiService(
  @Qualifier("nomisWebClientUtils") val serviceWebClientUtils: WebClientUtils,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getCaseloads(): CaseloadList {
    return serviceWebClientUtils.get("/reference-data/caseloads", CaseloadList::class.java)
  }
}

class CaseloadList : MutableList<PrisonCaseload> by ArrayList()

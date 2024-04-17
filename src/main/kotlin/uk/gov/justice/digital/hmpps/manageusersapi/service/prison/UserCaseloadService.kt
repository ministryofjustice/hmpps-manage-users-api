package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.CaseloadsApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseloadDetail

@Service("PrisonUserCaseloadService")
class UserCaseloadService(
  val caseloadsApiService: CaseloadsApiService,
) {

  fun getUserCaseloads(username: String): UserCaseloadDetail {
    return caseloadsApiService.getUserCaseloads(username)
  }
}

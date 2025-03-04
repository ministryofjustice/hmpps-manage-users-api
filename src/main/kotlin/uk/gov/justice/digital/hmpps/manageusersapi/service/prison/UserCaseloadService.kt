package uk.gov.justice.digital.hmpps.manageusersapi.service.prison

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.CaseloadsApiService
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseloadDetail

@Service("PrisonUserCaseloadService")
class UserCaseloadService(
  val caseloadsApiService: CaseloadsApiService,
) {

  fun getUserCaseloads(username: String): UserCaseloadDetail = caseloadsApiService.getUserCaseloads(username)

  fun addUserCaseloads(username: String, caseloads: List<String>): UserCaseloadDetail = caseloadsApiService.addUserCaseloads(username, caseloads)

  fun removeCaseloadFromUser(username: String, caseloadId: String): UserCaseloadDetail = caseloadsApiService.removeCaseloadFromUser(username, caseloadId)
}

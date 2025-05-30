package uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.UserCaseloadDetail

@Service(value = "nomisCaseloadsApiService")
class CaseloadsApiService(
  @Qualifier("nomisUserWebClientUtils") val userWebClientUtils: WebClientUtils,
  @Qualifier("nomisWebClientUtils") val serviceWebClientUtils: WebClientUtils,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getUserCaseloads(username: String) = serviceWebClientUtils.get("/users/{username}/caseloads", UserCaseloadDetail::class.java, username)

  fun addUserCaseloads(username: String, caseloads: List<String>): UserCaseloadDetail = userWebClientUtils.postWithResponse(
    "/users/{username}/caseloads",
    caseloads,
    UserCaseloadDetail::class.java,
    username,
  )

  fun removeCaseloadFromUser(username: String, caseloadId: String): UserCaseloadDetail = userWebClientUtils.deleteWithResponse(
    "/users/{username}/caseloads/{caseloadId}",
    UserCaseloadDetail::class.java,
    username,
    caseloadId,
  )
}

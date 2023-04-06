package uk.gov.justice.digital.hmpps.manageusersapi.adapter.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.EnabledExternalUser
import uk.gov.justice.digital.hmpps.manageusersapi.resource.external.DeactivateReason
import java.util.UUID

@Service(value = "externalUserApiService")
class UserApiService(
  @Qualifier("externalUsersUserWebClientUtils") val userWebClientUtils: WebClientUtils,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createUser(firstName: String, lastName: String, emailAddress: String, groupCodes: Set<String>?) =
    userWebClientUtils.postWithResponse(
      "/users/user/create",
      mapOf(
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to emailAddress,
        "groupCodes" to groupCodes,
      ),
      String::class.java,
    )

  fun updateUserEmailAddressAndUsername(userId: UUID, username: String, email: String) =
    userWebClientUtils.put("/users/id/$userId/email", mapOf("username" to username, "email" to email))

  fun hasPassword(userId: UUID) = userWebClientUtils.get("/users/id/$userId/password/present", Boolean::class.java)

  fun enableUserById(userId: UUID): EnabledExternalUser {
    log.debug("Enabling User for User Id of {} ", userId)
    return userWebClientUtils.putWithResponse("/users/$userId/enable", EnabledExternalUser::class.java)
  }

  fun disableUserById(userId: UUID, deactivateReason: DeactivateReason) {
    log.debug("Disabling User for User Id of {} ", userId)
    userWebClientUtils.put("/users/$userId/disable", deactivateReason)
  }
}

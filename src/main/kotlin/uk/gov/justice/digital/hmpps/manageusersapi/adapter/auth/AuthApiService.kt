package uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth

import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthService
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.AzureUser
import uk.gov.justice.digital.hmpps.manageusersapi.model.EmailAddress
import uk.gov.justice.digital.hmpps.manageusersapi.resource.PagedResponse
import uk.gov.justice.digital.hmpps.manageusersapi.service.Status
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthApiService(
  @Qualifier("authWebClientUtils") val serviceWebClientUtils: WebClientUtils,
  @Qualifier("authUserWebClientUtils") val userWebClientUtils: WebClientUtils,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun createNewToken(createTokenRequest: CreateTokenRequest): String {
    log.debug("Create Token for user, {}", createTokenRequest.username)
    return serviceWebClientUtils.postWithResponse(
      "/api/new-token",
      mapOf(
        "username" to createTokenRequest.username,
        "email" to createTokenRequest.email,
        "source" to createTokenRequest.source,
        "firstName" to createTokenRequest.firstName,
        "lastName" to createTokenRequest.lastName,
      ),
      String::class.java,
    )
  }

  fun createTokenByEmailType(tokenByEmailTypeRequest: TokenByEmailTypeRequest): String {
    log.debug("Create Token for user ${tokenByEmailTypeRequest.username} with email type ${tokenByEmailTypeRequest.emailType}")
    return serviceWebClientUtils.postWithResponse(
      "/api/token/email-type",
      mapOf("username" to tokenByEmailTypeRequest.username, "emailType" to tokenByEmailTypeRequest.emailType),
      String::class.java,
    )
  }

  fun createResetTokenForUser(userId: UUID) =
    serviceWebClientUtils.postWithResponse("/api/token/reset/$userId", String::class.java)

  fun findAuthUserEmail(username: String, unverified: Boolean) =
    serviceWebClientUtils.getIgnoreError("/api/user/$username/authEmail?unverified=$unverified", EmailAddress::class.java)

  fun syncEmailWithNomis(username: String, nomisEmail: String?) =
    serviceWebClientUtils.post("/api/prisonuser/$username/email/sync", mapOf("email" to nomisEmail))

  fun findAzureUserByUsername(username: String): AzureUser? =
    try {
      UUID.fromString(username)
      serviceWebClientUtils.getIgnoreError("/api/azureuser/$username", AzureUser::class.java)
    } catch (exception: IllegalArgumentException) {
      log.debug("Auth not called for Azure user as username not valid UUID: {}", username)
      null
    }

  fun findServiceByServiceCode(serviceCode: String) =
    serviceWebClientUtils.get("/api/services/$serviceCode", AuthService::class.java)

  fun findUserByUsernameAndSource(username: String, source: AuthSource): AuthUser =
    serviceWebClientUtils.get("/api/user/$username/$source", AuthUser::class.java)

  fun confirmRecognised(username: String) =
    userWebClientUtils.getWithEmptyResponseSucceeds("/api/user/$username/recognised")

  fun updateEmail(username: String, newEmailAddress: String) =
    userWebClientUtils.put("/api/prisonuser/$username/email", mapOf("email" to newEmailAddress))

  fun findUserEmails(usernames: List<String>): List<EmailAddress> = userWebClientUtils.postWithResponse(
    "/api/prisonuser/email",
    usernames,
    EmailList::class.java,
  )

  fun findUsers(
    name: String?,
    status: Status?,
    authSources: List<AuthSource>?,
    page: Int?,
    size: Int?,
    sort: String?,
  ) =
    userWebClientUtils.getWithParams(
      "/api/user/search",
      object : ParameterizedTypeReference<PagedResponse<AuthUserDto>>() {},
      mapNonNull(
        "name" to name,
        "status" to status,
        "authSources" to authSources,
        "page" to page,
        "size" to size,
        "sort" to sort,
      ),
    )
}

fun <K, V> mapNonNull(vararg pairs: Pair<K, V>) = mapOf(*pairs).filterValues { it != null }

class EmailList : MutableList<EmailAddress> by ArrayList()

data class TokenByEmailTypeRequest(
  val username: String,
  val emailType: String,
)

data class CreateTokenRequest(
  val username: String,
  val email: String,
  val source: String,
  val firstName: String,
  val lastName: String,
)

data class AuthUserDto(
  @Schema(description = "User ID", example = "91229A16-B5F4-4784-942E-A484A97AC865")
  val userId: UUID,

  @Schema(description = "Username", example = "externaluser")
  val username: String,

  @Schema(description = "Email address", example = "external.user@someagency.justice.gov.uk")
  val email: String,

  @Schema(description = "First name", example = "External")
  val firstName: String,

  @Schema(description = "Last name", example = "User")
  val lastName: String,

  @Schema(description = "Account is locked due to incorrect password attempts", example = "true")
  val locked: Boolean = false,

  @Schema(required = true, description = "Account is enabled", example = "false")
  val enabled: Boolean = false,

  @Schema(required = true, description = "Email address has been verified", example = "false")
  val verified: Boolean = false,

  @Schema(required = true, description = "Last time user logged in", example = "01/01/2001")
  val lastLoggedIn: LocalDateTime? = null,

  @Schema(required = true, description = "Authentication source", example = "delius")
  val source: String,
)

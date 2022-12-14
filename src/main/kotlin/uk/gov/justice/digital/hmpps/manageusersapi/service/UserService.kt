package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.DeactivateReason
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType.DPS_GEN
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.ExternalUsersApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.NomisApiService
import java.util.UUID

@Service
class UserService(
  private val nomisApiService: NomisApiService,
  private val tokenService: TokenService,
  private val verifyEmailDomainService: VerifyEmailDomainService,
  private val externalUsersApiService: ExternalUsersApiService,
  private val emailNotificationService: EmailNotificationService,
  private val telemetryClient: TelemetryClient,
) {
  @Throws(UserExistsException::class, TokenException::class, HmppsValidationException::class)
  fun createUser(user: CreateUserRequest): NomisUserDetails {
    if (!verifyEmailDomainService.isValidEmailDomain(user.email.substringAfter('@'))) {
      throw HmppsValidationException(user.email.substringAfter('@'), "Email domain not valid")
    }

    var nomisUserDetails: NomisUserDetails? = null
    if (DPS_ADM == user.userType) {
      nomisUserDetails = nomisApiService.createCentralAdminUser(user)
    } else if (DPS_GEN == user.userType) {
      nomisUserDetails = nomisApiService.createGeneralUser(user)
    } else if (DPS_LSA == user.userType) {
      nomisUserDetails = nomisApiService.createLocalAdminUser(user)
    }
    tokenService.saveAndSendInitialEmail(user, "DPSUserCreate")
    return nomisUserDetails ?: throw UserException(user.username, user.userType, "Error creating DPS User")
  }

  fun enableUserByUserId(userId: UUID) {
    val emailNotificationDto = externalUsersApiService.enableUserById(userId)
    emailNotificationDto.email?.let {
      emailNotificationService.sendEnableEmail(emailNotificationDto)
    } ?: run {
      log.warn("Notification email not sent for user {}", emailNotificationDto)
    }
    telemetryClient.trackEvent(
      "ExternalUserEnabled",
      mapOf("username" to emailNotificationDto.username, "admin" to emailNotificationDto.admin),
      null
    )
  }

  fun disableUserByUserId(userId: UUID, deactivateReason: DeactivateReason) =
    externalUsersApiService.disableUserById(userId, deactivateReason)

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

class UserExistsException(user: String, errorCode: String) :
  Exception("Unable to create user: $user with reason: $errorCode")

class HmppsValidationException(emailDomain: String, errorCode: String) :
  Exception("Invalid Email domain: $emailDomain with reason: $errorCode")

class UserException(user: String, userType: UserType, errorCode: String) :
  Exception("Unable to create user: $user of type $userType, with reason: $errorCode")

@Schema(description = "Nomis User Details")
data class NomisUserDetails(
  @Schema(description = "Username", example = "TEST_USER")
  val username: String,

  @Schema(description = "Email Address", example = "test@justice.gov.uk")
  val primaryEmail: String,

  @Schema(description = "First name of the user", example = "John")
  val firstName: String,

  @Schema(description = "Last name of the user", example = "Smith")
  val lastName: String,
)

data class EmailNotificationDto(
  @Schema(description = "Username", example = "TEST_USER")
  val username: String,

  @Schema(description = "First name of the user", example = "John")
  val firstName: String,

  @Schema(description = "email of the user", example = "Smith@gov.uk")
  val email: String?,

  @Schema(description = "admin id who enabled user", example = "ADMIN_USR")
  val admin: String,

)

package uk.gov.justice.digital.hmpps.manageusersapi.service.nomis

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType.DPS_GEN
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.TokenService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService

@Service("NomisUserService")
class UserService(
  private val nomisUserCreateService: UserApiService,
  private val tokenService: TokenService,
  private val verifyEmailDomainService: VerifyEmailDomainService,
) {
  @Throws(HmppsValidationException::class)
  fun createUser(user: CreateUserRequest): NomisUserDetails {
    if (!verifyEmailDomainService.isValidEmailDomain(user.email.substringAfter('@'))) {
      throw HmppsValidationException(user.email.substringAfter('@'), "Email domain not valid")
    }

    var nomisUserDetails: NomisUserDetails? = null
    if (DPS_ADM == user.userType) {
      nomisUserDetails = nomisUserCreateService.createCentralAdminUser(user)
    } else if (DPS_GEN == user.userType) {
      nomisUserDetails = nomisUserCreateService.createGeneralUser(user)
    } else if (DPS_LSA == user.userType) {
      nomisUserDetails = nomisUserCreateService.createLocalAdminUser(user)
    }
    tokenService.saveAndSendInitialEmail(user, "DPSUserCreate")
    return nomisUserDetails ?: throw UserException(user.username, user.userType, "Error creating DPS User")
  }
}

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

class HmppsValidationException(emailDomain: String, errorCode: String) :
  Exception("Invalid Email domain: $emailDomain with reason: $errorCode")

class UserException(user: String, userType: UserType, errorCode: String) :
  Exception("Unable to create user: $user of type $userType, with reason: $errorCode")

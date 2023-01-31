package uk.gov.justice.digital.hmpps.manageusersapi.service.nomis

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.CreateUserRequest
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
  fun createUser(user: CreateUserRequest): NomisUserCreatedDetails {
    if (!verifyEmailDomainService.isValidEmailDomain(user.email.substringAfter('@'))) {
      throw HmppsValidationException(user.email.substringAfter('@'), "Email domain not valid")
    }

    val nomisUserDetails = when (user.userType) {
      DPS_ADM -> nomisUserCreateService.createCentralAdminUser(user)
      DPS_GEN -> nomisUserCreateService.createGeneralUser(user)
      DPS_LSA -> nomisUserCreateService.createLocalAdminUser(user)
    }
    tokenService.saveAndSendInitialEmail(user, "DPSUserCreate")
    return nomisUserDetails
  }
}

@Schema(description = "Nomis User Created Details")
data class NomisUserCreatedDetails(
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

package uk.gov.justice.digital.hmpps.manageusersapi.service

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType.DPS_GEN
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType.DPS_LSA

@Service
class UserService(
  val nomisApiService: NomisApiService,
  val tokenService: TokenService,
  val authService: AuthService
) {
  @Throws(UserExistsException::class, TokenException::class, HmppsValidationException::class)
  @Transactional
  fun createUser(user: CreateUserRequest): NomisUserDetails {

    if (!validateEmailDomain(user.email.substringAfter('@')))
      throw HmppsValidationException(user.email.substringAfter('@'), "Email domain not valid")

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

  private fun validateEmailDomain(emailDomain: String): Boolean =
    authService.validateEmailDomain(emailDomain)
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

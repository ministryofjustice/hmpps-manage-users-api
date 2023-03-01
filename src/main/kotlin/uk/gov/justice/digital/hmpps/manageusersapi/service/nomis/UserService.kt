package uk.gov.justice.digital.hmpps.manageusersapi.service.nomis

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.auth.AuthApiService
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.NomisUserSummaryDto
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.nomis.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType.DPS_GEN
import uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis.UserType.DPS_LSA
import uk.gov.justice.digital.hmpps.manageusersapi.service.TokenService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService

@Service("NomisUserService")
class UserService(
  private val nomisUserApiService: UserApiService,
  private val authApiService: AuthApiService,
  private val tokenService: TokenService,
  private val verifyEmailDomainService: VerifyEmailDomainService,
) {
  @Throws(HmppsValidationException::class)
  fun createUser(user: CreateUserRequest): NomisUserCreatedDetails {
    if (!verifyEmailDomainService.isValidEmailDomain(user.email.substringAfter('@'))) {
      throw HmppsValidationException(user.email.substringAfter('@'), "Email domain not valid")
    }

    val nomisUserDetails = when (user.userType) {
      DPS_ADM -> nomisUserApiService.createCentralAdminUser(user)
      DPS_GEN -> nomisUserApiService.createGeneralUser(user)
      DPS_LSA -> nomisUserApiService.createLocalAdminUser(user)
    }
    tokenService.saveAndSendInitialEmail(user, "DPSUserCreate")
    return nomisUserDetails
  }

  fun findUsersByFirstAndLastName(firstName: String, lastName: String): List<PrisonUserDto> {
    val nomisUsers: List<NomisUserSummaryDto> = nomisUserApiService.findUsersByFirstAndLastName(firstName, lastName)
    // The users may have an unverified email, so we need to go to auth to determine if they are different
    if (nomisUsers.isNotEmpty()) {
      val authUsersByUsername = authApiService
        .findUserEmails(nomisUsers.map { it.username })
        .filter { !it.email.isNullOrBlank() }
        .associateBy { it.username }

      return nomisUsers.map {
        PrisonUserDto(
          username = it.username,
          userId = it.staffId,
          email = authUsersByUsername[it.username]?.email ?: it.email,
          verified = authUsersByUsername[it.username]?.verified ?: (it.email != null),
          firstName = it.firstName,
          lastName = it.lastName,
          activeCaseLoadId = it.activeCaseload?.id
        )
      }
    }
    return listOf()
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

data class PrisonUserDto(
  val username: String,
  val userId: String,
  val email: String?,
  val verified: Boolean,
  val firstName: String,
  val lastName: String,
  val activeCaseLoadId: String?
)

package uk.gov.justice.digital.hmpps.manageusersapi.service

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.resource.CreateUserRequest
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType.DPS_ADM
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType.DPS_GEN
import uk.gov.justice.digital.hmpps.manageusersapi.resource.UserType.DPS_LSA

@Service
class UserService(
  val nomisApiService: NomisApiService,
) {

  @Throws(UserExistsException::class)
  fun createUser(user: CreateUserRequest): NomisUserDetails = when (user.userType) {
    DPS_ADM -> nomisApiService.createCentralAdminUser(user)
    DPS_GEN -> nomisApiService.createGeneralUser(user)
    DPS_LSA -> nomisApiService.createLocalAdminUser(user)
  }
}

class UserExistsException(user: String, errorCode: String) :
  Exception("Unable to create user: $user with reason: $errorCode")

@Schema(description = "Nomis User Details")
data class NomisUserDetails(
  @Schema(description = "Username", example = "TEST_USER")
  val username: String,

  @Schema(description = "Email Address", example = "test@justice.gov.uk")
  val primaryEmail: String,

  @Schema(description = "First name of the user", example = "John")
  val firstName: String,

  @Schema(description = "Last name of the user", example = "Smith",)
  val lastName: String,
)

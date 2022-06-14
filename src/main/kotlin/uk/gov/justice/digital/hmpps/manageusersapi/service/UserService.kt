package uk.gov.justice.digital.hmpps.manageusersapi.service

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
  fun createUser(user: CreateUserRequest) {
    when (user.userType) {
      DPS_ADM -> nomisApiService.createCentralAdminUser(user)
      DPS_GEN -> nomisApiService.createGeneralUser(user)
      DPS_LSA -> nomisApiService.createLocalAdminUser(user)
    }
  }
}

class UserExistsException(user: String, errorCode: String) :
  Exception("Unable to create user: $user with reason: $errorCode")

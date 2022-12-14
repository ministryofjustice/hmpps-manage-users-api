package uk.gov.justice.digital.hmpps.manageusersapi.resource.nomis

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.manageusersapi.resource.NomisUserRolesController
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.UserRolesService

class UserRolesControllerTest {

  private val userRolesService: UserRolesService = mock()
  private val userRolesController = NomisUserRolesController(userRolesService)

  @Test
  fun `get user roles`() {

    userRolesController.getUserRoles("SOME_USER")
    verify(userRolesService).getUserRoles("SOME_USER")
  }
}

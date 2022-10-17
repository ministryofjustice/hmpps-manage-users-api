package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

class UserGroupServiceTest {

  private val externalUsersApiService: ExternalUsersApiService = mock()
  private lateinit var userGroupService: UserGroupService

  @BeforeEach
  fun setUp() {
    userGroupService = UserGroupService(externalUsersApiService)
  }

  @Test
  fun removeGroupByUserId() {
    val userId = UUID.randomUUID()
    userGroupService.removeGroupByUserId(userId, "test")
    verify(externalUsersApiService).deleteGroupByUserId(userId, "test")
  }
}

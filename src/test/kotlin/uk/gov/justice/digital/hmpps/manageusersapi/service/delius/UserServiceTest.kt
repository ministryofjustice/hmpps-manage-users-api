package uk.gov.justice.digital.hmpps.manageusersapi.service.delius

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.WebClientUtils
import uk.gov.justice.digital.hmpps.manageusersapi.adapter.delius.UserApiService
import uk.gov.justice.digital.hmpps.manageusersapi.config.DeliusRoleMappings
import uk.gov.justice.digital.hmpps.manageusersapi.model.DeliusUser

class UserServiceTest {

  private var webClient: WebClientUtils = mock()
  private lateinit var deliusService: UserApiService

  private val mappings = DeliusRoleMappings(
    mapOf(
      Pair("arole", listOf("role1", "role2")),
      Pair("test.role", listOf("role1", "role3")),
    ),
  )

  @BeforeEach
  fun setUp() {
    deliusService = UserApiService(webClient, mappings)
  }

  @Nested
  inner class GetDeliusUserByUsername {

    @Test
    fun `deliusUserByUsername test role mappings no roles granted`() {
      whenever(webClient.getIgnoreError(anyOrNull(), same(DeliusUser::class.java))).thenReturn(
        DeliusUser(
          username = "no_roles",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "TEST@DIGITAL.JUSTICE.GOV.UK",
          enabled = true,
          roles = listOf("NO_ROLES"),
        ),
      )
      val optionalDetails = deliusService.findUserByUsername("NO_ROLES")

      assertThat(optionalDetails).isEqualTo(
        DeliusUser(
          username = "NO_ROLES",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "test@digital.justice.gov.uk",
          enabled = true,
          roles = emptyList(),
        ),
      )
    }

    @Test
    fun `deliusUserByUsername test role mappings`() {
      whenever(webClient.getIgnoreError(anyOrNull(), same(DeliusUser::class.java))).thenReturn(
        DeliusUser(
          username = "deliussmith",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "TEST@DIGITAL.JUSTICE.GOV.UK",
          enabled = true,
          roles = listOf("TEST_ROLE"),
        ),
      )
      val optionalDetails = deliusService.findUserByUsername("DeliusSmith")
      assertThat(optionalDetails).isEqualTo(
        DeliusUser(
          username = "DELIUSSMITH",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "test@digital.justice.gov.uk",
          enabled = true,
          roles = listOf("role1", "role3"),
        ),
      )
    }

    @Test
    fun `deliusUserByUsername test username returned is upper cased`() {
      whenever(webClient.getIgnoreError(anyOrNull(), same(DeliusUser::class.java))).thenReturn(
        DeliusUser(
          username = "deliussmith",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "TEST@DIGITAL.JUSTICE.GOV.UK",
          enabled = true,
          roles = listOf("AROLE"),
        ),
      )
      val optionalDetails = deliusService.findUserByUsername("deliussmith")
      assertThat(optionalDetails).isEqualTo(
        DeliusUser(
          username = "DELIUSSMITH",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "test@digital.justice.gov.uk",
          enabled = true,
          roles = listOf("role1", "role2"),
        ),
      )
    }

    @Test
    fun `deliusUserByUsername test email returned is lower cased`() {
      whenever(webClient.getIgnoreError(anyOrNull(), same(DeliusUser::class.java))).thenReturn(
        DeliusUser(
          username = "DELIUS_mixed_Case",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "TEST@DIGITAL.JUSTICE.GOV.UK",
          enabled = true,
          roles = listOf("NO_ROLES"),
        ),
      )
      val optionalDetails = deliusService.findUserByUsername("DELIUS_MIXED_CASE")
      assertThat(optionalDetails).isEqualTo(
        DeliusUser(
          username = "DELIUS_MIXED_CASE",
          userId = "2500077027",
          firstName = "Delius",
          surname = "Smith",
          email = "test@digital.justice.gov.uk",
          enabled = true,
          roles = emptyList(),
        ),
      )
    }
  }
}

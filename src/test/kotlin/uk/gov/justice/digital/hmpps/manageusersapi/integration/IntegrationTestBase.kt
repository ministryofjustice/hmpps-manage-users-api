package uk.gov.justice.digital.hmpps.manageusersapi.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.manageusersapi.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock.DeliusApiMockServer
import uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock.ExternalUsersApiMockServer
import uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock.HmppsAuthMockServer
import uk.gov.justice.digital.hmpps.manageusersapi.integration.wiremock.NomisApiMockServer
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  companion object {
    @JvmField
    internal val nomisApiMockServer = NomisApiMockServer()

    @JvmField
    internal val hmppsAuthMockServer = HmppsAuthMockServer()

    @JvmField
    internal val externalUsersApiMockServer = ExternalUsersApiMockServer()

    @JvmField
    internal val deliusApiMockServer = DeliusApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      hmppsAuthMockServer.start()
      hmppsAuthMockServer.stubGrantToken()

      nomisApiMockServer.start()
      externalUsersApiMockServer.start()
      deliusApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      nomisApiMockServer.stop()
      hmppsAuthMockServer.stop()
      externalUsersApiMockServer.stop()
      deliusApiMockServer.stop()
    }
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
    authSource: AuthSource = AuthSource.auth
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  fun readFile(file: String): String = this.javaClass.getResource(file).readText()
}

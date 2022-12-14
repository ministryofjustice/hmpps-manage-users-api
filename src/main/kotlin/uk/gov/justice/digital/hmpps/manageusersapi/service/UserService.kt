package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.ExternalUsersApiService
import uk.gov.justice.digital.hmpps.manageusersapi.service.external.VerifyEmailDomainService
import uk.gov.justice.digital.hmpps.manageusersapi.service.nomis.NomisApiService

@Service
class UserService(
  private val nomisApiService: NomisApiService,
  private val tokenService: TokenService,
  private val verifyEmailDomainService: VerifyEmailDomainService,
  private val externalUsersApiService: ExternalUsersApiService,
  private val emailNotificationService: EmailNotificationService,
  private val telemetryClient: TelemetryClient,

) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

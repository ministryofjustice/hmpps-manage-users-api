package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.RateLimiter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.event.BulkUserJobItemMessage
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.UserRoleDetail
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserRolesService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.nio.charset.StandardCharsets
import java.util.Optional

class BulkUserJobItemRoleAssignmentServiceTest {
  private val bulkUserJobItemRepository: BulkUserJobItemRepository = mock()
  private val userRolesService: UserRolesService = mock()
  private val bulkUserJobReconciliationService: BulkUserJobReconciliationService = mock()
  private val rolesApiRateLimiter: RateLimiter = mock()
  private val auditService: HmppsAuditService = mock()
  private val objectMapper = ObjectMapper()
  private val service = BulkUserJobItemRoleAssignmentService(
    bulkUserJobItemRepository,
    userRolesService,
    bulkUserJobReconciliationService,
    rolesApiRateLimiter,
    auditService,
    objectMapper,
  )

  @Test
  fun `processes role assignment successfully`() {
    val (message, item) = createMessageAndItem()
    stubClaimAndLoad(item)
    whenever(userRolesService.addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")).thenReturn(createUserRoleDetail(item.username))
    whenever(
      bulkUserJobItemRepository.updateStatusAndResultIfCurrent(
        item.id,
        BulkUserJobItemStatus.STARTED,
        BulkUserJobItemStatus.SUCCESS,
        null,
        null,
      ),
    ).thenReturn(1)

    service.processRoleAssignmentMessage(message)

    verify(userRolesService).addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")
    verify(bulkUserJobItemRepository).updateStatusAndResultIfCurrent(item.id, BulkUserJobItemStatus.STARTED, BulkUserJobItemStatus.SUCCESS, null, null)
    verify(bulkUserJobReconciliationService).reconcileBulkJob(item.bulkUserJob.id)
  }

  @Test
  fun `publishes an audit event on successful role assignment`(): Unit = kotlinx.coroutines.runBlocking {
    val (message, item) = createMessageAndItem()
    stubClaimAndLoad(item)
    whenever(userRolesService.addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")).thenReturn(createUserRoleDetail(item.username))
    whenever(
      bulkUserJobItemRepository.updateStatusAndResultIfCurrent(
        item.id,
        BulkUserJobItemStatus.STARTED,
        BulkUserJobItemStatus.SUCCESS,
        null,
        null,
      ),
    ).thenReturn(1)

    service.processRoleAssignmentMessage(message)

    verifyAuditEventPublished("BULK_USER_ROLES_ASSIGN_ROLE_ATTEMPT", message, item)
    verifyAuditEventPublished("BULK_USER_ROLES_ASSIGN_ROLE_SUCCESS", message, item)
  }

  @Test
  fun `acquires a rate limiter permit before calling the roles api`() {
    val (message, item) = createMessageAndItem()
    stubClaimAndLoad(item)
    whenever(userRolesService.addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")).thenReturn(createUserRoleDetail(item.username))
    whenever(
      bulkUserJobItemRepository.updateStatusAndResultIfCurrent(
        item.id,
        BulkUserJobItemStatus.STARTED,
        BulkUserJobItemStatus.SUCCESS,
        null,
        null,
      ),
    ).thenReturn(1)

    service.processRoleAssignmentMessage(message)

    val ordered = inOrder(rolesApiRateLimiter, userRolesService)
    ordered.verify(rolesApiRateLimiter).acquire()
    ordered.verify(userRolesService).addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")
  }

  @Test
  fun `skips processing when item is in a terminal status`() {
    val (message, item) = createMessageAndItem()
    item.status = BulkUserJobItemStatus.SUCCESS
    whenever(bulkUserJobItemRepository.findById(item.id)).thenReturn(Optional.of(item))
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        item.id,
        BulkUserJobItemStatus.PUBLISHED,
        BulkUserJobItemStatus.STARTED,
        null,
      ),
    ).thenReturn(0)

    service.processRoleAssignmentMessage(message)

    verify(userRolesService, never()).addRolesToUserAsSystem(any(), any(), any())
    verify(bulkUserJobReconciliationService, never()).reconcileBulkJob(any())
  }

  @Test
  fun `skips processing when item no longer exists`() {
    val (message, item) = createMessageAndItem()
    whenever(bulkUserJobItemRepository.findById(item.id)).thenReturn(Optional.empty())

    service.processRoleAssignmentMessage(message)

    verify(bulkUserJobItemRepository, never()).updateStatusIfCurrent(any(), any(), any(), any())
    verify(userRolesService, never()).addRolesToUserAsSystem(any(), any(), any())
    verify(bulkUserJobReconciliationService, never()).reconcileBulkJob(any())
  }

  @Test
  fun `reprocesses item left started by an interrupted delivery`() {
    val (message, item) = createMessageAndItem()
    item.status = BulkUserJobItemStatus.STARTED
    whenever(bulkUserJobItemRepository.findById(item.id)).thenReturn(Optional.of(item))
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        item.id,
        BulkUserJobItemStatus.PUBLISHED,
        BulkUserJobItemStatus.STARTED,
        null,
      ),
    ).thenReturn(0)
    whenever(userRolesService.addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")).thenReturn(createUserRoleDetail(item.username))
    whenever(
      bulkUserJobItemRepository.updateStatusAndResultIfCurrent(
        item.id,
        BulkUserJobItemStatus.STARTED,
        BulkUserJobItemStatus.SUCCESS,
        null,
        null,
      ),
    ).thenReturn(1)

    service.processRoleAssignmentMessage(message)

    verify(userRolesService).addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")
    verify(bulkUserJobItemRepository).updateStatusAndResultIfCurrent(item.id, BulkUserJobItemStatus.STARTED, BulkUserJobItemStatus.SUCCESS, null, null)
    verify(bulkUserJobReconciliationService).reconcileBulkJob(item.bulkUserJob.id)
  }

  @Test
  fun `marks error when user is not found during role assignment`() {
    val (message, item) = createMessageAndItem()
    stubClaimAndLoad(item)
    whenever(userRolesService.addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")).thenThrow(notFoundException())
    whenever(
      bulkUserJobItemRepository.updateStatusAndResultIfCurrent(
        item.id,
        BulkUserJobItemStatus.STARTED,
        BulkUserJobItemStatus.ERROR,
        "User not found",
        null,
      ),
    ).thenReturn(1)

    service.processRoleAssignmentMessage(message)

    verify(bulkUserJobItemRepository).updateStatusAndResultIfCurrent(item.id, BulkUserJobItemStatus.STARTED, BulkUserJobItemStatus.ERROR, "User not found", null)
    verify(bulkUserJobReconciliationService).reconcileBulkJob(item.bulkUserJob.id)
    verifyAuditEventPublished("BULK_USER_ROLES_ASSIGN_ROLE_ATTEMPT", message, item)
    verifyAuditEventPublished("BULK_USER_ROLES_ASSIGN_ROLE_FAILURE", message, item, "User not found")
  }

  @Test
  fun `marks success when role is already assigned`() {
    val (message, item) = createMessageAndItem()
    stubClaimAndLoad(item)
    whenever(userRolesService.addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")).thenThrow(conflictException())
    whenever(
      bulkUserJobItemRepository.updateStatusAndResultIfCurrent(
        item.id,
        BulkUserJobItemStatus.STARTED,
        BulkUserJobItemStatus.SUCCESS,
        null,
        null,
      ),
    ).thenReturn(1)

    service.processRoleAssignmentMessage(message)

    verify(bulkUserJobItemRepository).updateStatusAndResultIfCurrent(
      item.id,
      BulkUserJobItemStatus.STARTED,
      BulkUserJobItemStatus.SUCCESS,
      null,
      null,
    )
    verify(bulkUserJobReconciliationService).reconcileBulkJob(item.bulkUserJob.id)
    verifyAuditEventPublished("BULK_USER_ROLES_ASSIGN_ROLE_ATTEMPT", message, item)
    verifyAuditEventPublished("BULK_USER_ROLES_ASSIGN_ROLE_REDUNDANT", message, item)
    verifyAuditEventNotPublished("BULK_USER_ROLES_ASSIGN_ROLE_SUCCESS")
    verifyAuditEventNotPublished("BULK_USER_ROLES_ASSIGN_ROLE_FAILURE")
  }

  @Test
  fun `marks system issue when assignment fails`() {
    val (message, item) = createMessageAndItem()
    stubClaimAndLoad(item)
    whenever(userRolesService.addRolesToUserAsSystem(item.username, listOf(item.rolename), "NWEB")).thenThrow(RuntimeException("boom"))
    whenever(
      bulkUserJobItemRepository.updateStatusAndResultIfCurrent(
        item.id,
        BulkUserJobItemStatus.STARTED,
        BulkUserJobItemStatus.ERROR,
        "System issue",
        null,
      ),
    ).thenReturn(1)

    service.processRoleAssignmentMessage(message)

    verify(bulkUserJobItemRepository).updateStatusAndResultIfCurrent(
      item.id,
      BulkUserJobItemStatus.STARTED,
      BulkUserJobItemStatus.ERROR,
      "System issue",
      null,
    )
    verify(bulkUserJobReconciliationService).reconcileBulkJob(item.bulkUserJob.id)
    verifyAuditEventPublished("BULK_USER_ROLES_ASSIGN_ROLE_ATTEMPT", message, item)
    verifyAuditEventPublished("BULK_USER_ROLES_ASSIGN_ROLE_FAILURE", message, item, "System issue")
  }

  private fun verifyAuditEventPublished(
    what: String,
    message: BulkUserJobItemMessage,
    item: BulkUserJobItem,
    error: String? = null,
  ): Unit = kotlinx.coroutines.runBlocking {
    verify(auditService).publishEvent(
      what = eq(what),
      subjectId = eq(item.username.uppercase()),
      subjectType = eq("USERNAME"),
      correlationId = eq(null),
      `when` = any(),
      who = eq(message.requestedBy),
      service = anyOrNull(),
      details = eq(
        objectMapper.writeValueAsString(
          buildMap<String, Any?> {
            put("role", item.rolename.uppercase())
            put("bulkUserJobId", message.jobId.toString())
            put("jiraReference", message.jiraReference)
            put("error", error)
          },
        ),
      ),
    )
  }

  private fun verifyAuditEventNotPublished(what: String): Unit = kotlinx.coroutines.runBlocking {
    verify(auditService, never()).publishEvent(
      what = eq(what),
      subjectId = anyOrNull(),
      subjectType = anyOrNull(),
      correlationId = anyOrNull(),
      `when` = any(),
      who = anyOrNull(),
      service = anyOrNull(),
      details = anyOrNull(),
    )
  }

  private fun stubClaimAndLoad(item: BulkUserJobItem) {
    whenever(
      bulkUserJobItemRepository.updateStatusIfCurrent(
        item.id,
        BulkUserJobItemStatus.PUBLISHED,
        BulkUserJobItemStatus.STARTED,
        null,
      ),
    ).thenReturn(1)
    whenever(bulkUserJobItemRepository.findById(item.id)).thenReturn(Optional.of(item))
  }

  private fun createMessageAndItem(username: String = "USER123", role: String = "ROLE_ONE"): Pair<BulkUserJobItemMessage, BulkUserJobItem> {
    val job = BulkUserJob(jiraReference = "JIRA-123", requestedBy = "userabc")
    val item = BulkUserJobItem(username = username, rolename = role, status = BulkUserJobItemStatus.PUBLISHED, bulkUserJob = job)
    val message = BulkUserJobItemMessage(
      jobId = job.id,
      jobItemId = item.id,
      username = username,
      rolename = role,
      jiraReference = job.jiraReference,
      requestedBy = job.requestedBy,
    )
    return message to item
  }

  private fun createUserRoleDetail(username: String) = UserRoleDetail(
    username = username,
    active = true,
    activeCaseload = uk.gov.justice.digital.hmpps.manageusersapi.resource.prison.PrisonCaseload("NWEB", "Nweb", "GENERAL"),
    dpsRoles = emptyList(),
    nomisRoles = emptyList(),
  )

  private fun notFoundException(): WebClientResponseException = WebClientResponseException.create(
    404,
    "Not Found",
    HttpHeaders.EMPTY,
    ByteArray(0),
    StandardCharsets.UTF_8,
  )

  private fun conflictException(): WebClientResponseException = WebClientResponseException.create(
    409,
    "Conflict",
    HttpHeaders.EMPTY,
    ByteArray(0),
    StandardCharsets.UTF_8,
  )
}

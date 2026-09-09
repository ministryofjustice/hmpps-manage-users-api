package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.util.concurrent.RateLimiter
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.event.BulkUserJobItemMessage
import uk.gov.justice.digital.hmpps.manageusersapi.model.DPS_CASELOAD
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserRolesService
import uk.gov.justice.hmpps.sqs.audit.HmppsAuditService
import java.util.UUID

@Service
class BulkUserJobItemRoleAssignmentService(
  private val bulkUserJobItemRepository: BulkUserJobItemRepository,
  private val userRolesService: UserRolesService,
  private val bulkUserJobReconciliationService: BulkUserJobReconciliationService,
  private val rolesApiRateLimiter: RateLimiter,
  private val auditService: HmppsAuditService,
  private val objectMapper: ObjectMapper,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val USER_NOT_FOUND = "User not found"
    private const val SYSTEM_ISSUE = "System issue"
    private const val ASSIGN_ROLE_ATTEMPT = "BULK_USER_ROLES_ASSIGN_ROLE_ATTEMPT"
    private const val ASSIGN_ROLE_SUCCESS = "BULK_USER_ROLES_ASSIGN_ROLE_SUCCESS"
    private const val ASSIGN_ROLE_FAILURE = "BULK_USER_ROLES_ASSIGN_ROLE_FAILURE"
    private const val ASSIGN_ROLE_REDUNDANT = "BULK_USER_ROLES_ASSIGN_ROLE_REDUNDANT"
  }

  fun processRoleAssignmentMessage(message: BulkUserJobItemMessage) {
    val item = bulkUserJobItemRepository.findById(message.jobItemId).orElse(null)
    if (item == null) {
      log.warn("Skipping bulk user job item {} because it no longer exists", message.jobItemId)
      return
    }

    if (!claimForAssignment(item)) {
      log.info(
        "Skipping bulk user job item {} because it is not awaiting assignment (already in a terminal state or not yet published)",
        message.jobItemId,
      )
      return
    }

    if (!messageMatchesPersistedItem(message, item)) {
      markError(item.id, SYSTEM_ISSUE)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
      log.warn("Received mismatched payload for bulk user job item {}", item.id)
      return
    }

    val username = item.username.uppercase()
    val roleCode = item.rolename.uppercase()

    throttleRolesApi(item.id)

    publishRoleAssignmentAuditEvent(ASSIGN_ROLE_ATTEMPT, message, username, roleCode)

    try {
      userRolesService.addRolesToUserAsSystem(username, listOf(roleCode), DPS_CASELOAD)
      // Publish the audit event before marking success so that, if auditing fails, the item is still STARTED and can
      // be transitioned to ERROR consistently (rather than being left SUCCESS while the listener retries/fails).
      publishRoleAssignmentAuditEvent(ASSIGN_ROLE_SUCCESS, message, username, roleCode)
      markSuccess(item.id)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
    } catch (e: WebClientResponseException.NotFound) {
      publishRoleAssignmentAuditEvent(ASSIGN_ROLE_FAILURE, message, username, roleCode, USER_NOT_FOUND)
      markError(item.id, USER_NOT_FOUND)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
    } catch (e: WebClientResponseException.Conflict) {
      publishRoleAssignmentAuditEvent(ASSIGN_ROLE_REDUNDANT, message, username, roleCode)
      // The user already has the role (either pre-existing, or assigned by a previous processing of this message that
      // failed before recording success), so treat it as a successful assignment
      markSuccess(item.id)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
    } catch (e: Exception) {
      publishRoleAssignmentAuditEvent(ASSIGN_ROLE_FAILURE, message, username, roleCode, SYSTEM_ISSUE)
      markError(item.id, SYSTEM_ISSUE)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
      log.error("Role assignment failed for bulk user job item {}", item.id, e)
    }
  }

  private fun publishRoleAssignmentAuditEvent(
    event: String,
    message: BulkUserJobItemMessage,
    username: String,
    roleCode: String,
    error: String? = null,
  ) {
    runBlocking {
      auditService.publishEvent(
        what = event,
        who = message.requestedBy,
        subjectId = username,
        subjectType = "USERNAME",
        correlationId = null,
        service = "hmpps-manage-users-api",
        details = objectMapper.writeValueAsString(
          BulkRoleAssignmentAuditDetails(
            role = roleCode,
            bulkUserJobId = message.jobId.toString(),
            jiraReference = message.jiraReference,
            error = error,
          ),
        ),
      )
    }
  }

  private fun throttleRolesApi(jobItemId: UUID) {
    val waitedSeconds = rolesApiRateLimiter.acquire()
    if (waitedSeconds > 0) {
      log.debug("Throttled Roles API call for bulk user job item {} - waited {}s for a permit", jobItemId, waitedSeconds)
    }
  }

  private fun claimForAssignment(item: BulkUserJobItem): Boolean {
    // This might be a recovery so if the item was already started then no need to set the status as started,
    // otherwise claim by changing from published to started.
    val alreadyStarted = item.status == BulkUserJobItemStatus.STARTED
    if (!alreadyStarted) {
      val claimedFromPublished = bulkUserJobItemRepository.updateStatusIfCurrent(
        jobItemId = item.id,
        currentStatus = BulkUserJobItemStatus.PUBLISHED,
        newStatus = BulkUserJobItemStatus.STARTED,
      ) == 1
      if (claimedFromPublished) {
        return true
      }
    }
    return alreadyStarted
  }

  private fun markSuccess(jobItemId: UUID) {
    val updatedRows = bulkUserJobItemRepository.updateStatusAndResultIfCurrent(
      jobItemId = jobItemId,
      currentStatus = BulkUserJobItemStatus.STARTED,
      newStatus = BulkUserJobItemStatus.SUCCESS,
    )
    check(updatedRows == 1) { "Bulk user job item $jobItemId could not be marked as SUCCESS from STARTED" }
  }

  private fun markError(jobItemId: UUID, reason: String) {
    val updatedRows = bulkUserJobItemRepository.updateStatusAndResultIfCurrent(
      jobItemId = jobItemId,
      currentStatus = BulkUserJobItemStatus.STARTED,
      newStatus = BulkUserJobItemStatus.ERROR,
      result = reason,
    )
    check(updatedRows == 1) { "Bulk user job item $jobItemId could not be marked as ERROR from STARTED" }
  }

  private fun messageMatchesPersistedItem(message: BulkUserJobItemMessage, item: BulkUserJobItem): Boolean = message.jobId == item.bulkUserJob.id &&
    message.username.equals(item.username, ignoreCase = true) &&
    message.rolename.equals(item.rolename, ignoreCase = true)
}

private data class BulkRoleAssignmentAuditDetails(
  val role: String,
  val bulkUserJobId: String,
  val jiraReference: String,
  val error: String? = null,
)

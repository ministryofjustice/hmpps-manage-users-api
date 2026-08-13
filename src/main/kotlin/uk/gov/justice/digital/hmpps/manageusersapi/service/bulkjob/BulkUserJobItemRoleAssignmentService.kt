package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.manageusersapi.event.BulkUserJobItemMessage
import uk.gov.justice.digital.hmpps.manageusersapi.model.DPS_CASELOAD
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItemStatus
import uk.gov.justice.digital.hmpps.manageusersapi.service.prison.UserRolesService
import java.util.UUID

@Service
class BulkUserJobItemRoleAssignmentService(
  private val bulkUserJobItemRepository: BulkUserJobItemRepository,
  private val userRolesService: UserRolesService,
  private val bulkUserJobReconciliationService: BulkUserJobReconciliationService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val USER_NOT_FOUND = "User not found"
    private const val ROLE_ALREADY_ASSIGNED = "Role already assigned"
    private const val SYSTEM_ISSUE = "System issue"
  }

  fun processRoleAssignmentMessage(message: BulkUserJobItemMessage) {
    if (!claimForAssignment(message.jobItemId)) {
      log.info("Skipping bulk user job item {} because it is not in PUBLISHED status", message.jobItemId)
      return
    }

    val item = bulkUserJobItemRepository.findById(message.jobItemId)
      .orElseThrow { IllegalStateException("Bulk user job item ${message.jobItemId} not found") }

    if (!messageMatchesPersistedItem(message, item)) {
      markError(item.id, SYSTEM_ISSUE)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
      log.warn("Received mismatched payload for bulk user job item {}", item.id)
      return
    }

    val username = item.username.uppercase()
    val roleCode = item.rolename.uppercase()

    try {
      userRolesService.addRolesToUser(username, listOf(roleCode), DPS_CASELOAD)
      markSuccess(item.id)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
    } catch (e: WebClientResponseException.NotFound) {
      markError(item.id, USER_NOT_FOUND)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
    } catch (e: WebClientResponseException.Conflict) {
      markError(item.id, ROLE_ALREADY_ASSIGNED)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
    } catch (e: Exception) {
      markError(item.id, SYSTEM_ISSUE)
      bulkUserJobReconciliationService.reconcileBulkJob(item.bulkUserJob.id)
      log.error("Role assignment failed for bulk user job item {}", item.id, e)
    }
  }

  private fun claimForAssignment(jobItemId: UUID): Boolean = bulkUserJobItemRepository.updateStatusIfCurrent(
    jobItemId = jobItemId,
    currentStatus = BulkUserJobItemStatus.PUBLISHED,
    newStatus = BulkUserJobItemStatus.STARTED,
  ) == 1

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

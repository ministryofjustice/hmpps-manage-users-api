package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.validation.ValidationException
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.manageusersapi.event.BulkJobPublisher
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobDetails
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.resource.bulkjob.BulkUserRoleAdditionsRequest
import uk.gov.justice.digital.hmpps.manageusersapi.service.EntityNotFoundException
import java.io.Writer
import java.util.UUID

@Service
class BulkUserJobService(
  private val bulkUserJobRepository: BulkUserJobRepository,
  private val bulkUserJobItemRepository: BulkUserJobItemRepository,
  private val bulkJobPublisher: BulkJobPublisher,
  private val telemetryClient: TelemetryClient,
  @param:Value("\${application.bulk-jobs.throttling.large-batch-warning-threshold}") private val largeBatchWarningThreshold: Int,
) {
  companion object {
    private const val USER_ID_HEADER = "userId"
    private const val LARGE_BATCH_EVENT = "BulkRoleAssignmentLargeBatch"
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun createBulkUserRoleAdditionsJob(
    usersCsv: MultipartFile,
    bulkJobDetails: BulkUserRoleAdditionsRequest,
    requestedBy: String,
  ): UUID {
    val users = parseFileForUsers(usersCsv)
    val bulkJob = createAndPersistJob(bulkJobDetails, requestedBy, users)
    publishAfterCommit(bulkJob)
    return bulkJob.id
  }

  private fun publishAfterCommit(bulkJob: BulkUserJob) {
    // Ensure publish only happens if the job has been persisted so we do not try to process before that has happened
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
        object : TransactionSynchronization {
          override fun afterCommit() {
            try {
              bulkJobPublisher.publishBulkUserJobEvent(bulkJob)
            } catch (e: Exception) {
              // The job and its items are already persisted at this point, so do not expose this error to the caller
              // so they still receives the job id - the scheduled reconciler will republish the unprocessed CREATED items.
              log.error("Failed to publish bulk user job event for job {} after commit - leaving for reconciliation", bulkJob.id, e)
            }
          }
        },
      )
    } else {
      bulkJobPublisher.publishBulkUserJobEvent(bulkJob)
    }
  }

  fun getBulkUserRoleAdditionsJobs(search: String, pageNumber: Int?, pageSize: Int?): Page<BulkUserJob> {
    var pagination = Pageable.unpaged(Sort.by("RequestDateTime").descending())

    if (pageNumber != null && pageSize != null) {
      pagination = PageRequest.of(pageNumber, pageSize, Sort.by("RequestDateTime").descending())
    }

    return bulkUserJobRepository.findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase(
      jiraReference = search,
      requestedBy = search,
      pageable = pagination,
    )
  }

  fun getBulkUserRoleAdditionsJobDetails(id: UUID): BulkUserJobDetails? = bulkUserJobRepository.findDetailsById(id)

  @Transactional
  fun writeJobResultsToCsv(writer: Writer, jobId: UUID) {
    bulkUserJobRepository.findByIdOrNull(jobId)?.let {
      bulkUserJobRepository.findCompletedJobById(jobId)?.let { _ ->
        writer.write("userId,roleCode,status,reason\n")

        bulkUserJobItemRepository.streamByBulkUserJobId(jobId).use { stream ->
          stream.forEach { item ->
            writer.write(item.toCsvRow())
          }
        }
        writer.flush()
      } ?: throw BulkUserJobNotCompleteException(jobId)
    } ?: throw BulkUserJobNotFoundException(jobId)
  }

  private fun BulkUserJobItem.toCsvRow(): String = "$username,$rolename,$status,${result ?: ""}\n"

  private fun createAndPersistJob(
    bulkJobDetails: BulkUserRoleAdditionsRequest,
    requestedBy: String,
    users: List<String>,
  ): BulkUserJob {
    val bulkJob = BulkUserJob(jiraReference = bulkJobDetails.jiraReference, requestedBy = requestedBy)
    users.forEach { user ->
      bulkJobDetails.roles.forEach { role ->
        bulkJob.addItem(user, role)
      }
    }
    bulkUserJobRepository.save(bulkJob)
    warnIfLargeBatch(bulkJob)
    return bulkJob
  }

  private fun warnIfLargeBatch(bulkJob: BulkUserJob) {
    val itemCount = bulkJob.jobItems.size
    if (itemCount > largeBatchWarningThreshold) {
      telemetryClient.trackEvent(
        LARGE_BATCH_EVENT,
        mapOf(
          "bulkJobId" to bulkJob.id.toString(),
          "itemCount" to itemCount.toString(),
          "warningThreshold" to largeBatchWarningThreshold.toString(),
        ),
        null,
      )
      log.warn(
        "Large bulk user job {} accepted with {} role assignments (warning threshold {})",
        bulkJob.id,
        itemCount,
        largeBatchWarningThreshold,
      )
    }
  }

  private fun parseFileForUsers(userCsv: MultipartFile): List<String> {
    val rows = userCsv.inputStream.bufferedReader().use { reader ->
      val csvFormat = CSVFormat.Builder.create().setTrim(true).build()
      csvFormat.parse(reader).map { record: CSVRecord ->
        if (record.size() != 1) {
          throw ValidationException("Users csv row does not have exactly 1 column")
        }
        record.first()
      }.toList()
    }

    val users = rows.dropHeaderRowIfPresent()
    if (users.isEmpty()) {
      throw ValidationException("Users csv does not contain any rows")
    }
    return users
  }

  private fun List<String>.dropHeaderRowIfPresent(): List<String> = if (isNotEmpty() && first().equals(USER_ID_HEADER, ignoreCase = true)) {
    drop(1)
  } else {
    this
  }
}

class BulkUserJobNotFoundException(val id: UUID) : EntityNotFoundException("Bulk user job $id not found")

class BulkUserJobNotCompleteException(val id: UUID) : Exception("unable to generate bulk user download csv: job $id is not complete")

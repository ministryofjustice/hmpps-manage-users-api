package uk.gov.justice.digital.hmpps.manageusersapi.service.bulkjob

import jakarta.validation.ValidationException
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.manageusersapi.event.BulkJobPublisher
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkUserJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkUserJob
import uk.gov.justice.digital.hmpps.manageusersapi.resource.bulkjob.BulkUserRoleAdditionsRequest
import java.util.UUID

@Service
class BulkUserJobService(
  private val bulkUserJobRepository: BulkUserJobRepository,
  private val bulkJobPublisher: BulkJobPublisher,
) {
  @Transactional
  fun createBulkUserRoleAdditionsJob(
    usersCsv: MultipartFile,
    bulkJobDetails: BulkUserRoleAdditionsRequest,
    requestedBy: String,
  ): UUID {
    val users = parseFileForUsers(usersCsv)
    val bulkJob = createAndPersistJob(bulkJobDetails, requestedBy, users)
    bulkJobPublisher.publishBulkUserJobEvent(bulkJob)
    return bulkJob.id
  }

  fun getBulkUserRoleAdditionsJobs(search: String, pageNumber: Int?, pageSize: Int?): List<BulkUserJob> {
    var pagination = Pageable.unpaged(Sort.by("RequestDateTime").descending())
    if (pageNumber != null && pageSize != null) {
      pagination = PageRequest.of(pageNumber, pageSize, Sort.by("RequestDateTime").descending())
    }

    return bulkUserJobRepository.findByJiraReferenceContainingIgnoreCaseOrRequestedByContainingIgnoreCase(
      jiraReference = search,
      requestedBy = search,
      pageable = pagination,
    ).content
  }

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
    return bulkJob
  }

  private fun parseFileForUsers(userCsv: MultipartFile): List<String> {
    val users = userCsv.inputStream.bufferedReader().use { reader ->
      val csvFormat = CSVFormat.Builder.create().setTrim(true).build()
      csvFormat.parse(reader).map { record: CSVRecord ->
        if (record.size() != 1) {
          throw ValidationException("Users csv row does not have exactly 1 column")
        }
        record.first()
      }.toList()
    }
    if (users.isEmpty()) {
      throw ValidationException("Users csv does not contain any rows")
    }
    return users
  }
}

package uk.gov.justice.digital.hmpps.manageusersapi.resource

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.manageusersapi.listeners.model.BulkJobEvent
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkJobItemRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.BulkJobRepository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkJob
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkJobItem
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkJobItemStatus
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.util.UUID

@RestController
@RequestMapping("/bulk-jobs", produces = [MediaType.APPLICATION_JSON_VALUE])
class BulkUsersController(
  private val bulkJobRepository: BulkJobRepository,
  private val bulkJobItemRepository: BulkJobItemRepository,
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
) {

  data class AddRolesToUsersInstance(
    val id: UUID,
    val jiraReference: String,
    val status: String,
    val allJobs: Long,
    val completedJobs: Long,
    val erroredJobs: Long,
  )

  @GetMapping("/user-role-additions")
  fun getUserRoleAdditionsJobs(): ResponseEntity<List<AddRolesToUsersInstance>> {
    val results: MutableList<AddRolesToUsersInstance> = mutableListOf()

    bulkJobRepository.findAll()
      .forEach { job ->
        results.add(
          AddRolesToUsersInstance(
            id = job.id,
            jiraReference = job.jiraReference,
            status = job.status.toString(),
            completedJobs = bulkJobItemRepository.countByBulkJobIdAndStatusIs(job.id, BulkJobItemStatus.Completed),
            erroredJobs = bulkJobItemRepository.countByBulkJobIdAndStatusIs(job.id, BulkJobItemStatus.Errored),
            allJobs = bulkJobItemRepository.countByBulkJobId(job.id),
          )
        )
      }

    return ResponseEntity.status(200).body(results)
  }

  data class Body(val userIds: List<String>, val roles: List<String>, val jiraReference: String)

  @PostMapping("/user-role-additions")
  fun createUserRoleAdditionsJob(
    @RequestBody body: Body,
  ): ResponseEntity<String> {
    
    val bulkJob = bulkJobRepository.saveAndFlush(BulkJob(
      jiraReference = body.jiraReference,
      numberOfRoles = body.roles.size,
      numberOfUsers = body.userIds.size,
      requestedBy = "user",
    ))
    
    body.roles.forEach { role ->
      body.userIds.forEach { userId ->
        bulkJobItemRepository.save(BulkJobItem(
          username = userId,
          rolename = role,
          bulkJob = bulkJob,
        ))
    }}
   
    val bulkJobQueue = hmppsQueueService.findByQueueId("bulkjobqueue")
    bulkJobQueue?.sqsClient?.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(bulkJobQueue.queueUrl)
        .messageBody(objectMapper.writeValueAsString(BulkJobEvent(id = bulkJob.id)))
        .build(),
    )
    
    return ResponseEntity.status(202).body("Bulk add roles to users job started")
  }
}

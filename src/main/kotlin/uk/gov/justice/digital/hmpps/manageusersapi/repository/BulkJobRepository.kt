package uk.gov.justice.digital.hmpps.manageusersapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.manageusersapi.repository.model.BulkJob
import java.util.UUID

@Repository
interface BulkJobRepository : JpaRepository<BulkJob, UUID>

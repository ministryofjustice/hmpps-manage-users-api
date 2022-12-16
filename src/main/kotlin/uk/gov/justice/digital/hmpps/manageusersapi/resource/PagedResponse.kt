package uk.gov.justice.digital.hmpps.manageusersapi.resource

data class PagedResponse<T>(
  val content: List<T>,
  val pageable: PageDetails,
  val last: Boolean,
  val totalPages: Int,
  val totalElements: Long,
  val size: Int,
  val number: Int,
  val sort: PageSort,
  val numberOfElements: Int,
  val first: Boolean,
  val empty: Boolean,
)

data class PageDetails(
  val sort: PageSort,
  val offset: Int,
  val pageNumber: Int,
  val pageSize: Int,
  val paged: Boolean,
  val unpaged: Boolean
)

data class PageSort(
  val sorted: Boolean,
  val unsorted: Boolean,
  val empty: Boolean,
)

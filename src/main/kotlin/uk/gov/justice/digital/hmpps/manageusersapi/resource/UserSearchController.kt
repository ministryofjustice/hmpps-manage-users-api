package uk.gov.justice.digital.hmpps.manageusersapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.manageusersapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.manageusersapi.model.AuthSource
import uk.gov.justice.digital.hmpps.manageusersapi.service.Status
import uk.gov.justice.digital.hmpps.manageusersapi.service.UserSearchService

@RestController("lUserSearchController")
class UserSearchController(
  private val userSearchService: UserSearchService,
) {

  @GetMapping("/users/search")
  @Operation(
    summary = "Search for users ",
    description = """
      Search for users in the Auth DB only who match on partial first name, surname, username or email and return a pageable result set. 
      Optionally choose the authentication sources from any combination of auth, delius, nomis and azuread sources.
      It will default to AuthSource.auth if the authSources parameter is omitted.
      Provide the authSources as a list of values with the same name. e.g. ?authSources=nomis&authSources=delius&authSources=auth
      It will return users with the requested auth sources where they have authenticated against the auth service at least once.
      Note: User information held in the auth service may be out of date with the user information held in the source systems as
      their details will be as they were the last time that they authenticated.
    """,
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "OK",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize(
    "hasAnyRole('ROLE_INTEL_ADMIN', 'ROLE_PCMS_USER_ADMIN', 'ROLE_PF_USER_ADMIN')",
  )
  fun searchForUsersInMultipleSourceSystems(
    @Parameter(description = "The username, email or name of the user.", example = "j smith")
    @RequestParam(required = false)
    name: String?,
    @Parameter(description = "User status to find ACTIVE, INACTIVE or ALL. Defaults to ALL if omitted.")
    @RequestParam(required = false)
    status: Status?,
    @Parameter(description = "List of auth sources to search [nomis|delius|auth|azuread]. Defaults to auth if omitted.")
    @RequestParam(required = false)
    authSources: List<AuthSource>?,
    @RequestParam(value = "page", required = false) page: Int?,
    @RequestParam(value = "size", required = false) size: Int?,
    @RequestParam(value = "sort", required = false) sort: String?,
  ) = userSearchService.searchUsers(
    name,
    status,
    authSources,
    page,
    size,
    sort,
  )
}

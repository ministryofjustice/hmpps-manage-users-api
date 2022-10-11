# hmpps-manage-users-api
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=for-the-badge&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-manage-users)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-manage_users "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-manage-users-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-manage-users-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-manage-users-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-manage-users-api)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://manage-users-api.hmpps.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config)

A Spring Boot JSON API to manage the users. Backend services for https://github.com/ministryofjustice/manage-hmpps-auth-accounts

### Building

```./gradlew build```

### Code style & formatting
```bash
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook
```
will apply ktlint styles to intellij and also add a pre-commit hook to format all changed kotlin files.

#### Health

- `/health/ping`: will respond `{"status":"UP"}` to all requests.  This should be used by dependent systems to check connectivity to hmpps-manage-users-api,
  rather than calling the `/health` endpoint.
- `/health`: provides information about the application health and its dependencies.  This should only be used
  by hmpps-manage-users-api health monitoring (e.g. pager duty) and not other systems who wish to find out the state of hmpps-manage-users-api.
- `/info`: provides information about the version of deployed application.

### Architecture

Architecture decision records start [here](doc/architecture/decisions/0001-use-adr.md)

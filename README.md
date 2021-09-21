# hmpps-manage-users-api
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-manage-users-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-manage-users-api)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://hmpps-manage-users-api.hmpps.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config)

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

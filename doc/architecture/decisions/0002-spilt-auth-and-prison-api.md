# 2. Create APIs for user/roles management separate from prison API and auth API

[Next >>](9999-end.md)


Date: 2021-11-23

## Status

Accepted

## Context

`HMPPS Auth` is currently deployed to the Azure Kubernetes Service (AKS) cluster.
This is required since it has direct access to the NOMIS database.

`HMPPS Manage Users` also requires access to users held within the NOMIS database and the HMPPS Auth database (users external to HMPPS), therefore this service has a dependency on both `HMPPS Auth` and the `prison-api`.

So that `HMPPS Auth` can be migrated to the MoJ strategic platform the dependency on the NOMIS Oracle database needs to be removed since we do not currently have a proven way of accessing the NOMIS Oracle database directly from an AWS account into Azure.

`prison-api` is currently a large service since it covers most of the NOMIS domain. This causes contention when releasing new versions since many teams are also committing to this repository. It is also hard to write effective unit tests since the integration tests rely on pre-built seed data that is loaded both when running the service locally and when running integration tests, this data is also hard to amend so creating flexible tests cases is difficult.


## Decision

We will remove the dependency that HMPPS Auth  has on the NOMIS Oracle database by ensuring all access to that data is via a new API
- `nomis-user-roles-api`

The name indicates that this API is responsible for user data in NOMIS and would eventually become redundant when the NOMIS data is moved.

#### NOMIS User Roles API

This requires privileged access to the NOMIS Oracle database so that it can manage passwords. So that privileged access can eventually be removed from `prison-api` the current functions in `prison-api` related to the management of user, roles and passwords will be migrated to this API.
The new endpoint will be written in the preferred Kotlin language and use more flexible integration tests techniques; such as creating and disposing of data required for a unit test dynamically.

The existing endpoints in `prison-api` will be deprecated and removed once there are no more clients calling those endpoints

We will remove the dependency of HMPPS Auth from HMPPS Manage Users instead it will use the new API
- `hmpps-manage-users-api`

#### HMPPS Manage Users API

Functionality related to managing user accounts will be moved to this API. The API will orchestrate the updates for this data; for instance, if a role needs updating in NOMIS it will use the `nomis-user-roles-api` to apply the update and also the `HMPPS Auth` API to apply the update there as well. This allows the `hmpps-manage-users` UI service to remain ignorant on the source of data being updated so that it can be simplified.


## Consequences

By adding the additional APIs we expect:

- Release cycle for the NOMIS user features can be shortened so can be developed and released quicker
- The `prison-api` will be simplified and more focussed on prisoner related endpoints
- HMPPS Auth can be migrated to a platform with better support then currently available in the Azure AKS cluster
- HMPPS Manage Users is simplified as orchestration is moved to `hmpps-manage-users-api`
- A future iteration of this shifting of functionality is a `user-management-service` which would be the gateway to external users and roles which. This would allow HMPPS Auth to be decoupled from the external user datastore.

[Next >>](9999-end.md)

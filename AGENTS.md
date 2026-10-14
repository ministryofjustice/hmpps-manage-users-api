# hmpps-manage-users-api

A Kotlin/Spring Boot JSON API that is the backend for https://github.com/ministryofjustice/hmpps-manage-users.
It aggregates and unifies user/role/group data from several upstream HMPPS identity systems (HMPPS Auth,
NOMIS/prison-api, Delius) behind a single API.

## Build, test, lint

- Use Java 25 with the standard Gradle wrapper.
- Assemble without running checks: `./gradlew assemble`
- Run tests with `./gradlew test`; do not run `./gradlew check` locally because it includes ktlint tasks.
- Run a single test class: `./gradlew test --tests "uk.gov.justice.digital.hmpps.manageusersapi.service.UserServiceTest"`
- Run a single test method (names are backtick strings): `./gradlew test --tests "uk.gov.justice.digital.hmpps.manageusersapi.service.UserServiceTest.find external user"`
- Do not run any ktlint Gradle task, including formatting, checks, or pre-commit hook setup. Skip ktlint validation entirely.
- The full test suite requires localstack (SQS); the `test` profile uses an in-memory H2 database. Isolated unit tests such
  as the examples above do not need localstack. CI starts it; locally use
  `docker-compose -f docker-compose-test.yml up -d localstack`.
- Integration tests use WireMock stubs under `wiremock/mappings` (not the docker-compose auth service) and a
  `test` Spring profile — see `src/test/kotlin/.../integration/IntegrationTestBase.kt` and
  `integration/wiremock/*MockServer.kt` for `NomisApiMockServer`, `HmppsAuthMockServer`,
  `ExternalUsersApiMockServer`, `DeliusApiMockServer`.

## Architecture

The codebase proxies/combines four identity "sources", modelled by the `AuthSource` enum
(`model/AuthSource.kt`): `auth` (HMPPS external-users service), `nomis` (prison-api), `delius`
(community-api), and `azuread`. Almost every layer is organised per-source:

- `resource/` — Spring MVC `@RestController`s (REST API, Swagger annotations, `@PreAuthorize`).
  Source-specific controllers live in subpackages `resource/external`, `resource/prison`,
  `resource/bulkjob`; generic/combined endpoints (e.g. `UserController`, `UserSearchController`) sit at
  the top level and dispatch based on `AuthSource`.
- `service/` — business logic. Generic/aggregate services that combine all sources live at the top level
  (`UserService`, `RolesService`, `UserSearchService`); source-specific services live in subpackages
  `service/external` (HMPPS Auth), `service/prison` (NOMIS), and `service/bulkjob`. There is no dedicated
  `service/delius` package — Delius logic sits directly in `adapter/delius`. Several classes share the same
  name (for example, `UserService`) across different packages, so always check the package, not just
  the class name, when searching.
- `adapter/` — outbound `WebClient`-based clients to the real upstream services, one subpackage per
  source: `adapter/auth`, `adapter/nomis`, `adapter/delius`, `adapter/external`, plus `adapter/email`
  (GOV.UK Notify) and `WebClientUtils.kt`.
- `config/` — one `WebClientConfiguration` per upstream (`AuthWebClientConfiguration`,
  `NomisWebClientConfiguration`, `DeliusWebClientConfiguration`, `ExternalUsersWebClientConfiguration`),
  each built on `AbstractWebClientConfiguration` and configured via OAuth2 client credentials
  (`OAuth2ClientConfiguration`, `ClientCachingOAuth2AuthorizedClientService`).
- `model/` — DTOs shared across sources, including `GenericUser`, the unified user representation that
  source-specific adapters map into.
- `repository/` — Spring Data JPA entities/repositories backed by Postgres (Flyway migrations in
  `src/main/resources/db/migration`), used for allowlisting/local persistence rather than user identity
  itself.
- `event/` — SQS-based domain events (via hmpps-sqs-spring-boot-starter).
- `scheduled/` — scheduled jobs (e.g. bulk/allowlist housekeeping).

A typical "find user" flow: a generic controller/service (e.g. `UserService.findUserByUsernameWithAuthSource`)
switches on `AuthSource` and delegates to the matching per-source service/adapter, normalising the result to
`GenericUser`. When adding a new user-facing feature, consider whether it needs equivalent handling in all
four sources or is source-specific.

## Conventions

- Kotlin, ktlint with `intellij_idea` code style (`.editorconfig`); some enum entries intentionally use
  lower-case names matching external API values and are annotated with
  `@Suppress("ktlint:standard:enum-entry-name-case", "standard:enum-entry-name-case")` — follow this pattern
  rather than renaming such entries.
- Test classes mirror the main package structure 1:1 under `src/test/kotlin`, and reuse the exact class name
  of the class under test (e.g. `service/external/UserService.kt` → `service/external/UserServiceTest.kt`).
- Test method names are backtick-quoted descriptive sentences (JUnit 5), e.g.
  `` fun `find external user`() ``.
- Controllers use shared Swagger response annotations `@AuthenticatedApiResponses` /
  `@StandardApiResponses` (`resource/swagger`) instead of repeating `@ApiResponses` boilerplate, and
  authentication context comes from `HmppsAuthenticationHolder` (from `hmpps-kotlin-spring-boot-starter`),
  not `SecurityContextHolder` directly.
- Outbound calls to upstream APIs go through the per-source `adapter` `WebClient` services — do not call
  `WebClient` directly from controllers/services.

## Dependency injection

Prefer constructor injection over field injection in Spring components, including test classes.

- In production code, declare dependencies as constructor parameters (e.g. `private val fooRepository: FooRepository`) rather than using `@Autowired` on a field.
- In integration test classes, pass `@Autowired` repositories/beans as primary constructor parameters instead of using `@Autowired` on a `lateinit var` field.

## Tests

Prefer `@BeforeAll`/`@AfterAll` over `@BeforeEach`/`@AfterEach` where possible in nested integration test classes.

- Use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` on the nested test class so that `@BeforeAll`/`@AfterAll` can be non-static (instance) methods.
- Build shared fixture data once in `@BeforeAll` and clean it up once in `@AfterAll`, rather than rebuilding it before every test, when the fixture is not mutated in a way that would leak between tests.
- When copying or adapting an integration test, preserve the full nested test structure, including `Security`, `Validation`, and `HappyPath` scenarios for each endpoint.
- Place each `@AfterAll` method immediately after the corresponding `@BeforeAll` setup method.
- OAuth2 propagation tests must verify the exact WireMock header value with `equalTo("Bearer ABCDE")`; do not weaken this assertion to a regex or non-empty-header matcher.

## Linting

Do not run `ktlintCheck` (or any ktlint gradle task) in this repository. Skip ktlint validation entirely.

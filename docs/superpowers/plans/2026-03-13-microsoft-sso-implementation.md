# Microsoft SSO Integration Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Microsoft SSO with full server-side and client-side parity while preserving existing Google flows and safely migrating the backend from email/Google-shaped identity to provider-aware identity.

**Architecture:** Implement this in three layers: first make persistence and auth contracts rollout-safe for Google-only production traffic, then add Microsoft backend capabilities behind hard gates, then ship frontend runtime-config and MSAL support without regressing the existing Google experience. The rollout must explicitly separate Release 1 dual-write, Release 2 backfill/constraints, and Release 3 Phase A/B/C JWT-auth-code cutover before Microsoft can be enabled.

**Tech Stack:** Spring Boot 3.5, Spring Security OAuth2 Client, Spring Security JOSE, JPA/Hibernate, Flyway, Redis, PostgreSQL, Testcontainers, React 19, Vite 7, Playwright, `@react-oauth/google`, `@azure/msal-browser`, `@azure/msal-react`

---

This stays as **one plan** because the database migration, JWT/auth-code compatibility window, backend feature gates, and frontend runtime config are tightly coupled. Splitting those apart would create non-shippable intermediate states.

Before implementation starts, create or switch to a **dedicated git worktree** for this feature branch.

Use these helper skills during execution:
- `@superpowers:subagent-driven-development`
- `@superpowers:test-driven-development`
- `@superpowers:verification-before-completion`
- `@superpowers:systematic-debugging`

## Chunk 1: Backend rollout-safe identity foundation

### File structure map

#### Backend files to modify

| File | Purpose | Planned change |
|---|---|---|
| `backend/pom.xml:1-94` | dependencies | add Flyway, Testcontainers, and Microsoft JWT verification support |
| `backend/src/main/resources/application.yml:1-34` | runtime config | add Flyway config, auth rollout flags, Microsoft flags, and public provider-config properties |
| `backend/src/test/resources/application.yml:1-31` | test config | add safe test defaults for rollout mode and Microsoft-disabled startup |
| `backend/src/main/java/com/demo/sso/model/User.java:192-249` | user entity | add provider-aware fields while keeping `google_id` during compatibility releases |
| `backend/src/main/java/com/demo/sso/repository/UserRepository.java:256-259` | repository API | add provider-aware and ID-based lookup methods |
| `backend/src/main/java/com/demo/sso/service/UserService.java:271-310` | user upsert logic | add Release 1 dual-write behavior, Release 2 provider-aware reads, and profile lookup by ID |
| `backend/src/main/java/com/demo/sso/service/JwtTokenService.java:399-469` | JWT logic | support legacy and `ver=2` contracts with explicit mint modes |
| `backend/src/main/java/com/demo/sso/config/JwtAuthenticationFilter.java:526-556` | auth filter | dual-resolution during Phase A/B, `User.id` steady state in Phase C |
| `backend/src/main/java/com/demo/sso/controller/UserController.java:160-185` | effective `/api/user/me` contract | move from `findByEmail` to provider-aware/current-user resolution and return `provider`, `providerUserId`, and `lastLoginFlow` |
| `backend/src/main/java/com/demo/sso/controller/AuthController.java:75-147` | auth endpoints | keep Google working, then add Microsoft challenge/verify and DTOs |
| `backend/src/main/java/com/demo/sso/config/SecurityConfig.java:13-57` | security config | reserve Microsoft routes, add runtime gate behavior, and configure cookie-based OAuth request storage |
| `backend/src/main/java/com/demo/sso/service/OAuth2SuccessHandler.java:330-381` | OAuth success flow | dual-write Google first, then provider-aware success handling for both providers |
| `backend/src/main/java/com/demo/sso/service/AuthCodeStore.java:1-18` | auth-code contract | support legacy and `v2` storage/read modes |
| `backend/src/main/java/com/demo/sso/service/RedisAuthCodeStore.java:480-508` | auth-code Redis implementation | add mint/read mode support for `authcode:*` and `authcode:v2:*` |
| `backend/src/main/java/com/demo/sso/service/GoogleTokenVerifier.java:1-28` | Google verify edge | normalize into shared provider-aware identity path without regressing current validation |

#### Backend files to create

| File | Responsibility |
|---|---|
| `backend/src/main/java/com/demo/sso/model/AuthProvider.java` | `GOOGLE` / `MICROSOFT` enum |
| `backend/src/main/java/com/demo/sso/model/AuthFlow.java` | `SERVER_SIDE` / `CLIENT_SIDE` enum |
| `backend/src/main/java/com/demo/sso/model/IdentityContractMode.java` | rollout mode enum such as `LEGACY_ONLY`, `COMPATIBILITY`, `V2_ONLY` |
| `backend/src/main/java/com/demo/sso/service/NormalizedIdentity.java` | provider-neutral identity payload |
| `backend/src/main/java/com/demo/sso/service/AuthenticatedUserIdentity.java` | parsed JWT identity object for legacy/v2 filtering |
| `backend/src/main/java/com/demo/sso/service/ProviderIdentityNormalizer.java` | exact Google/Microsoft claim normalization rules |
| `backend/src/main/java/com/demo/sso/config/AuthRolloutProperties.java` | typed rollout flags for JWT mint mode and contract mode |
| `backend/src/main/java/com/demo/sso/config/IdentityContractGuard.java` | startup/runtime enforcement for Microsoft enablement and `V2_ONLY` |
| `backend/src/main/java/com/demo/sso/service/MicrosoftTokenVerifier.java` | Microsoft ID token verification |
| `backend/src/main/java/com/demo/sso/service/MicrosoftOidcUserService.java` | tenant-aware server-side Microsoft OIDC validation for `common` entry with final issuer enforcement |
| `backend/src/main/java/com/demo/sso/service/MicrosoftChallengeStore.java` | challenge issuance/consume contract |
| `backend/src/main/java/com/demo/sso/service/RedisMicrosoftChallengeStore.java` | Redis-backed Microsoft challenge store |
| `backend/src/main/java/com/demo/sso/config/MicrosoftAuthProperties.java` | Microsoft config + public runtime-config fields |
| `backend/src/main/java/com/demo/sso/config/OAuth2AuthorizationRequestCookieRepository.java` | signed/encrypted cookie-based authorize→callback continuity |
| `backend/src/main/java/com/demo/sso/controller/ProviderConfigController.java` | effective `/api/auth/providers` endpoint |
| `backend/src/main/java/com/demo/sso/controller/dto/AuthCodeExchangeRequest.java` | DTO for `/auth/exchange` |
| `backend/src/main/java/com/demo/sso/controller/dto/ProviderTokenVerifyRequest.java` | DTO for Google verify |
| `backend/src/main/java/com/demo/sso/controller/dto/MicrosoftChallengeResponse.java` | DTO for challenge response |
| `backend/src/main/java/com/demo/sso/controller/dto/MicrosoftVerifyRequest.java` | DTO for Microsoft verify |
| `backend/src/main/resources/db/migration/V2__add_provider_identity_columns.sql` | Release 1 nullable provider columns only |
| `backend/src/main/resources/db/migration/B1__pre_provider_google_schema.sql` | baseline migration describing the current pre-provider-aware schema for empty/new environments |
| `backend/src/main/resources/db/migration/V3__backfill_google_provider_identity.sql` | Release 2 backfill for existing Google rows |
| `backend/src/main/resources/db/migration/V4__drop_email_unique_add_provider_unique.sql` | Release 2 constraints after backfill |

#### Backend tests to add or update

| File | Coverage |
|---|---|
| `backend/src/test/java/com/demo/sso/migration/FlywayPostgresMigrationTest.java` | PostgreSQL-backed migration path from current schema |
| `backend/src/test/java/com/demo/sso/service/UserServiceTest.java` | dual-write, provider-aware reads, and no-duplicate-email invariant before Phase C |
| `backend/src/test/java/com/demo/sso/service/JwtTokenServiceTest.java` | legacy/Phase A/Phase B/Phase C token rules |
| `backend/src/test/java/com/demo/sso/service/RedisAuthCodeStoreTest.java` | namespace mint/read behavior across rollout modes |
| `backend/src/test/java/com/demo/sso/service/OAuth2SuccessHandlerTest.java` | Google regression + Microsoft callback fail-closed behavior |
| `backend/src/test/java/com/demo/sso/controller/ControllerIntegrationTest.java` | `/api/user/me`, `/api/auth/exchange`, provider-field contract, and Microsoft disabled responses |
| `backend/src/test/java/com/demo/sso/service/MicrosoftTokenVerifierTest.java` | issuer/audience/tid/guest rejection, non-ID-token rejection, and required claim-profile validation |
| `backend/src/test/java/com/demo/sso/service/ProviderIdentityNormalizerTest.java` | `iss|sub` provider user ID, email precedence, normalization rules, `emailVerified=false`, and `#EXT#` / `idp` rejection |
| `backend/src/test/java/com/demo/sso/service/RedisMicrosoftChallengeStoreTest.java` | challenge TTL, replay rejection, and rate limits |
| `backend/src/test/java/com/demo/sso/controller/ProviderConfigControllerTest.java` | runtime provider config schema + cache headers |
| `backend/src/test/java/com/demo/sso/config/IdentityContractGuardTest.java` | startup fail-fast and hard-disabled behavior |

## Notes

See the approved source plan in the original workspace for the full task-by-task execution detail. This copy exists in the isolated worktree so implementation, verification, PR preparation, and review can reference the approved plan locally.

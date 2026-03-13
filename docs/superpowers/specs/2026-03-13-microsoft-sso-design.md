# Microsoft SSO Integration — Design Document

**Date:** 2026-03-13
**Status:** Approved for planning
**Related existing feature:** Google SSO integration with dual OAuth2 flows

## Overview

Extend the existing Google SSO demo to support **Microsoft SSO** with **full parity** across both authentication flows:

1. **Server-side OAuth2 authorization code flow** handled by Spring Security
2. **Client-side token flow** initiated in the React frontend and verified by the backend

The Microsoft integration should support **personal Microsoft accounts and work/school member/home-tenant accounts that provide a usable email-like identifier under the documented normalization rules**, and it should follow the same top-level UX pattern as Google by adding explicit Microsoft login cards alongside the existing Google ones.

A Microsoft login and a Google login with the same email address must be treated as **separate accounts**. Identity uniqueness is therefore based on provider identity, not email.

## Product decisions

The design reflects the following approved decisions:

- **Scope:** full Microsoft parity with Google
- **Account linking:** none; same email across providers remains separate accounts
- **Frontend layout:** explicit cards per provider and flow
- **Microsoft audience:** personal Microsoft accounts and work/school member/home-tenant accounts that provide a usable email-like identifier under the documented normalization rules
- **Implementation style:** hybrid approach
  - backend becomes provider-aware and normalized
  - frontend remains explicit and readable rather than heavily abstracted

## Goals

- Preserve all existing Google SSO behavior
- Add Microsoft SSO support for both server-side and client-side flows for accounts that satisfy the documented identity requirements
- Refactor the backend and data model so authentication is no longer Google-specific
- Keep the demo easy to understand by showing each provider/flow explicitly
- Reuse shared JWT, Redis auth-code exchange, and frontend auth state handling wherever practical

## Non-goals

- Merging Google and Microsoft identities into one user account
- Introducing a generic pluggable provider framework for arbitrary future providers
- Replacing the current home page with a dynamic/provider-config-driven UI
- Large unrelated refactors outside auth/provider boundaries

## Recommended approach

Use a **hybrid implementation**:

- normalize provider-specific identities in the backend
- keep provider-specific entry points at the integration edge
- reuse shared business logic for user upsert, JWT issuance, and auth-code exchange
- keep the frontend explicit with four auth cards:
  - Google server-side
  - Google client-side
  - Microsoft server-side
  - Microsoft client-side

This approach fixes the parts of the current codebase that are too Google-specific while keeping the UI simple, readable, and aligned with the demo’s educational purpose.

## Current-state constraints

The current implementation is strongly Google-shaped:

- backend OAuth registration only includes Google
- `AuthController` exposes Google-specific verification paths
- `User` stores `googleId`
- `UserService` upserts by Google identity
- frontend shows only Google login components/cards
- docs, labels, and tests assume Google-only behavior

The Microsoft design must preserve existing behavior while removing Google-only assumptions from the shared backend model.

## Target architecture

### Shared auth concepts

Introduce provider-neutral authentication concepts:

- `AuthProvider`: `GOOGLE`, `MICROSOFT`
- `AuthFlow`: `SERVER_SIDE`, `CLIENT_SIDE`

Introduce a normalized internal identity payload used after provider verification succeeds. This payload should include:

- `provider`
- `providerUserId`
- `email`
- `emailVerified`
- `name`
- `pictureUrl` (optional)
- `authFlow`

This normalized payload becomes the handoff point between provider-specific verification logic and shared persistence/JWT logic.

### Provider claim mapping

The normalization step must define exact mapping rules so both OAuth flows for a provider produce the same stored identity.

| Normalized field | Google source | Microsoft source | Rule |
|---|---|---|---|
| `provider` | constant `GOOGLE` | constant `MICROSOFT` | Fixed by integration path |
| `providerUserId` | Google `sub` | Microsoft issuer-scoped subject: `iss + "|" + sub` | Must be stable across both Microsoft flows; server-side and client-side normalization must use the same canonical rule |
| `email` | `email` | Microsoft email claim precedence: `email` → `preferred_username` → `upn` | Login is rejected if no acceptable normalized email-like value is available |
| `emailVerified` | `email_verified` | always `false` / unsupported for Microsoft normalization | Microsoft login does not infer verified-email state from token claims alone |
| `name` | `name` | Microsoft display name claim if present | Optional display value |
| `pictureUrl` | `picture` | optional / provider-specific photo source | Optional; absence must not block login |
| `authFlow` | request context | request context | `SERVER_SIDE` or `CLIENT_SIDE` |

Microsoft-specific normalization requirements:

- both Microsoft flows must resolve to the **same canonical `providerUserId` rule**
- Microsoft canonical `providerUserId` is the issuer-scoped value `iss + "|" + sub` for both server-side and client-side flows
- the application must use a single Microsoft app registration/client identity across both flows so the app-scoped subject remains stable
- login must be rejected if Microsoft identity data does not contain a usable email under the accepted precedence rules
- for Microsoft identities, successful ID token validation is sufficient for login, but `emailVerified` remains unsupported/`false` because Microsoft token claims are not treated as a verified-email signal

### Server-side flow

Keep the existing secure pattern:

1. frontend navigates to provider-specific Spring Security OAuth endpoint
2. provider authenticates the user and redirects back to the backend callback
3. Spring Security completes token exchange
4. provider-specific claims are normalized into the shared internal identity payload
5. backend upserts the user using provider-aware identity rules
6. backend generates a JWT
7. backend stores the JWT in Redis and returns a single-use auth code
8. backend redirects to frontend with `?code=...`
9. frontend exchanges the code for the JWT using the existing exchange endpoint

The Redis-based single-use code exchange remains unchanged in principle and continues to prevent JWT leakage via redirect URLs.

Rolling-deploy continuity requirement for server-side OAuth:

- authorize-to-callback correlation must not depend on node-local memory during rolling deploys
- the application should use a signed/encrypted cookie-based OAuth2 authorization-request store so the callback can land on any node and still complete safely without sticky routing
- if a session-based authorization-request store is retained instead, shared session state or sticky routing is mandatory before mixed-version rollout is allowed

Server-side Microsoft gating requirement:

- Microsoft server-side feature-flag checks must be enforced both at authorize-entry time and again in the OAuth callback/success path
- the route `/api/oauth2/authorization/microsoft` must always be reserved by application code so disabled/unconfigured behavior is deterministic
- when Microsoft server-side auth is disabled or unconfigured, that reserved route must return `503 Service Unavailable` with `{"error":"Microsoft server-side login is disabled"}` instead of falling through to `404`
- if Microsoft server-side login becomes disabled before callback completion, the callback path must fail closed before normalization, user upsert, JWT issuance, or auth-code creation
- callback-time disablement must redirect to `${FRONTEND_URL}/?error=provider_disabled&provider=microsoft&flow=server_side` and must not mint any JWT, auth code, or authenticated session artifact
- callback-time disablement must also explicitly clear any transient authenticated state created during callback processing, including invalidating the OAuth/session state, clearing `SecurityContext`, and expiring any related session cookie before redirecting

### Client-side flow

Keep provider-specific verification endpoints at the API boundary while sharing the downstream logic:

- Google client-side flow continues using the Google browser SDK and backend verify endpoint
- Microsoft client-side flow uses MSAL in the frontend, a backend-issued single-use challenge/nonce, and a dedicated backend verify endpoint
- both verification endpoints normalize claims into the same internal payload
- both then reuse the same provider-aware user upsert and JWT issuance flow

### Microsoft client-side protocol contract

The Microsoft client-side flow must be specified tightly enough to implement and verify safely.

Required contract:

- frontend uses **MSAL Browser / MSAL React**
- frontend performs Microsoft sign-in using the standard Microsoft identity platform browser flow
- frontend requests at minimum the OIDC scopes `openid profile email`
- the Microsoft app registration is configured to return the standard ID token claims needed for normalization, with `email` requested when available
- before starting Microsoft client-side sign-in, frontend fetches a backend-issued single-use challenge from `POST /api/auth/microsoft/challenge`
- frontend includes the backend-issued nonce in the MSAL sign-in request
- frontend sends the resulting **Microsoft ID token** and challenge identifier to the backend for authentication
- backend verifies **only ID tokens** for the Microsoft client-side auth endpoint
- backend rejects Microsoft Graph access tokens or any token type not explicitly intended for application authentication

Required backend validation for Microsoft client-side tokens:

- signature validation against Microsoft identity metadata/JWKS
- expiry validation
- audience validation against this application’s Microsoft client ID
- issuer validation against the accepted Microsoft issuer set for the configured audience mode
- tenant/audience mode validation consistent with supporting personal accounts and organizational member/home-tenant accounts only
- required ID-token claim profile validation: token must contain the expected browser sign-in identity claims, including `iss`, `sub`, `aud`, `ver`, and `tid`
- nonce validation against the backend-issued stored challenge
- single-use consumption of the stored challenge on successful verification
- reject tokens that show access-token-only/resource-token semantics, such as `scp`-driven API access patterns without the expected ID-token claim profile
- the shared Microsoft app registration used for browser sign-in must not rely on app-specific API scopes under the same audience used for this identity-token authentication path

### Acceptable Microsoft email extraction contract

The same normalization rule must be used across both Microsoft flows.

- preferred normalized claim order is `email`, then `preferred_username`, then `upn`
- normalization trims surrounding whitespace and lowercases the stored value
- accepted values must satisfy a single conservative validator: exactly one `@`, no internal whitespace, and non-empty local-part and domain-part segments
- if none of the candidate claims contains an acceptable normalized value, Microsoft login is rejected
- when normalization falls back to `preferred_username` or `upn`, the value may be stored/displayed as the account email/login identifier, but it must not be treated as a verified-email signal
- for organizational accounts, normalized identifiers that contain guest-style markers such as `#EXT#` must be rejected

Examples:

- accept: `User@Example.com` → stored as `user@example.com`
- reject: `userexample.com`
- reject: `user @example.com`
- reject: blank or whitespace-only values

### Microsoft authority strategy

- use the Microsoft identity platform **v2.0** endpoints
- configure the application as a **multi-tenant app supporting both personal and organizational accounts**
- support scope is limited to personal accounts and organizational **member/home-tenant** accounts; guest/B2B identities are out of scope for this release and must be rejected
- use the shared audience mode consistent with Microsoft’s `common` sign-in entry point for interactive sign-in
- backend validation must only accept Microsoft ID tokens whose issuer exactly matches `https://login.microsoftonline.com/{tid}/v2.0`, where `{tid}` is the token tenant ID claim
- for personal accounts, the accepted tenant ID is `9188040d-6c67-4c5b-b112-36a304b66dad`
- for organizational accounts, the authenticated principal must represent a home-tenant/member account; guest/B2B identities are rejected for this release even if Microsoft token validation otherwise succeeds
- organizational tokens must include `tid`, and accepted member/home-tenant accounts must satisfy all of the following:
  - issuer tenant matches the token `tid`
  - normalized login identifier does not contain `#EXT#`
  - if the `idp` claim is present, it must represent the same tenant authority as the token issuer rather than an external/home identity provider
- the same authority and audience assumptions must be used by both Spring Security server-side login and backend client-side ID token verification
- for Spring Security server-side login, use `common` only as the interactive authorize entry point; token validation must use tenant-independent v2.0 discovery and custom issuer validation rather than a single pinned tenant issuer
- `common`, `organizations`, and `consumers` are interactive authorize-entry authorities only and are never acceptable final token issuer values

## Data model design

### User identity model

Refactor the `User` entity away from provider-specific storage.

Recommended fields:

- `id`
- `provider`
- `provider_user_id`
- `email`
- `name`
- `picture_url`
- `last_login_flow`
- `created_at`
- `last_login_at`

Recommended uniqueness rule:

- unique on **`(provider, provider_user_id)`**

Email must become **non-unique** at the database level so the same email can exist for separate Google and Microsoft accounts.

Behavioral implications:

- Google identity uniqueness is scoped to Google
- Microsoft identity uniqueness is scoped to Microsoft
- the same email may exist in multiple user rows when sourced from different providers
- until Release 3 / Phase C completes and legacy unversioned JWTs are no longer accepted, application behavior must preserve a temporary invariant of **at most one active row per normalized email** so legacy email-subject JWT resolution remains deterministic
- therefore Microsoft account creation must remain disabled, and no migration/backfill step may create duplicate-email rows, during any period when legacy JWTs are still accepted

`last_login_flow` is an audit/display field only. It is not part of identity matching, uniqueness, or authorization decisions.

### Migration strategy

The current `google_id` column must be migrated into the new provider-aware identity shape.

Recommended migration behavior:

- existing Google users are preserved and mapped to:
  - `provider = GOOGLE`
  - `provider_user_id = existing google_id`
- Google logins continue to match those existing users after the refactor
- Microsoft logins create distinct rows even when email matches an existing Google user
- the existing unique constraint/index on `email` must be removed before Microsoft same-email separate accounts can be supported safely
- because current databases were created by Hibernate, the migration must deterministically discover and drop the email uniqueness object by column membership or first normalize it to a known name in a preparatory migration

Because this release must drop the unique `email` constraint, add/backfill provider-aware identity columns, and preserve existing users safely, this change must use **Flyway** as an explicit versioned database migration mechanism for rollout. Do not rely on `ddl-auto: update` for this release. Outside tests, set `spring.jpa.hibernate.ddl-auto=validate` once Flyway-managed migrations are introduced.

Flyway baseline contract:

- introduce Flyway with an explicit baseline strategy for existing Hibernate-created databases
- existing non-empty databases must receive a schema history table through a **controlled one-time Flyway baseline process**; do not rely on ad hoc `baselineOnMigrate` behavior in production
- define the initial Flyway baseline version as the current pre-provider-aware schema
- operationally, production rollout must: (1) back up the database, (2) run the one-time baseline step to create Flyway history for the current schema, (3) verify the recorded baseline version matches the expected pre-provider-aware schema, and only then (4) apply the staged provider-aware migrations
- rollback for baseline adoption is: stop the rollout, restore from backup or remove the newly created Flyway history entry only under controlled operator procedure, and keep the pre-Flyway application version in service
- add an upgrade test proving migration from a schema produced by today’s running Hibernate-managed application version to the new provider-aware schema

Required migration sequence:

1. **Release 1:** add nullable `provider` and `provider_user_id` columns via Flyway
2. **Release 1:** deploy code that **dual-writes** legacy `google_id` and the new provider-aware columns for Google-authenticated users
3. **Release 2:** backfill all existing Google users to `provider = GOOGLE` and `provider_user_id = google_id`
4. **Release 2:** remove the legacy unique constraint/index on `email`
5. **Release 2:** validate that no existing Google row has null or duplicate provider identity after backfill
6. **Release 2:** add the unique constraint on `(provider, provider_user_id)` and make the new columns non-null
7. **Release 3 / Phase A:** switch reads fully to the provider-aware columns while still accepting legacy auth artifacts
8. **Release 3 / Phase B:** mint only `v2` JWTs and `authcode:v2:*` while all nodes continue accepting legacy artifacts during the compatibility window
9. **Release 3 / Phase C:** after the maximum legacy JWT TTL and auth-code TTL expire, reject legacy artifacts and only then enable Microsoft sign-in
10. **Release 4 or later:** retire `google_id` after the new identity path is proven stable in runtime

Rollback requirement:

- if backfill validation fails, stop the migration and keep the old column authoritative until the data issue is resolved

### JWT/auth cutover rule

- because the current system uses `sub = email` and the new system uses `sub = internal User.id`, deployment must not silently mix the two identity contracts
- the current live legacy format is: JWT with **no `ver` claim**, `sub = email`, and Redis auth codes stored under `authcode:*`
- the rollout moves to an explicit **JWT contract version `v2`** in the release that switches `sub = User.id`
- provider-aware JWTs must carry an explicit version claim `ver=2`
- cutover must use a **compatibility-first rollout** for rolling deployment safety:
  - **Phase A:** deploy all nodes able to accept both the legacy unversioned format and `v2`, while still minting legacy JWTs and storing auth codes under `authcode:*`; during this phase, legacy JWTs continue resolving users by email, while `ver=2` JWTs resolve users by `User.id`
  - **Phase B:** after fleet convergence, switch minting/storage to `v2` JWTs and `authcode:v2:*` while all nodes still accept both the legacy format and `v2`; during this phase, legacy JWTs still resolve users by email, while `ver=2` JWTs resolve users by `User.id`
  - **Phase C:** after the maximum legacy JWT TTL and auth-code TTL have expired, reject legacy unversioned JWTs and ignore or delete `authcode:*`
- `JwtAuthenticationFilter` must treat `ver=2` as the new contract and treat missing `ver` as legacy; legacy JWTs are rejected only in Phase C
- `/api/user/me` and any authenticated user-loading path must use dual-resolution during Phases A and B: missing-`ver` JWTs resolve by legacy email subject, and `ver=2` JWTs resolve by internal `User.id`; email-based resolution is removed only in Phase C
- Redis auth-code exchange keys rotate from legacy `authcode:*` to `authcode:v2:*`, with legacy acceptance allowed through the compatibility window
- during Phases A and B, `/api/auth/exchange` must probe both the legacy `authcode:*` namespace and the `authcode:v2:*` namespace so mixed-fleet rollout can redeem either artifact type
- users will be required to sign in again after the identity-contract deployment

### Microsoft enablement gate

- Microsoft server-side and client-side login must remain disabled until all identity reads are provider-aware end to end
- specifically, Microsoft must not be enabled until JWT `sub = User.id` is live, `/user/me` resolves by `User.id`, provider-aware reads/writes are fully deployed, old JWTs and pending auth-code payloads are invalidated, and the email unique constraint has been removed

Required backend invariant:

- backend must expose an explicit identity-contract mode such as `identityContractMode=V2_ONLY` before Microsoft can be advertised as enabled
- when either Microsoft feature flag is `true` while the V2-only invariant is false, backend startup must fail fast with a clear configuration error
- when both Microsoft feature flags are `false` while the V2-only invariant is false, backend starts normally and `/api/auth/providers` reports Microsoft hard-disabled
- Microsoft may be advertised as enabled only when all of the following are true:
  - JWT `ver=2` is the only accepted contract
  - legacy unversioned JWTs are rejected
  - legacy `authcode:*` entries are no longer redeemable
  - `/user/me` resolves by `User.id`

Required feature flags:

- backend flags `app.auth.microsoft.server-side-enabled` and `app.auth.microsoft.client-side-enabled`, both default `false`
- when `app.auth.microsoft.server-side-enabled=false`, `GET /api/oauth2/authorization/microsoft` must reject with `503 Service Unavailable` and JSON body `{"error":"Microsoft server-side login is disabled"}`
- when `app.auth.microsoft.client-side-enabled=false`, `POST /api/auth/microsoft/challenge` must reject with `503 Service Unavailable` and JSON body `{"error":"Microsoft client-side login is disabled"}`
- when `app.auth.microsoft.client-side-enabled=false`, `POST /api/auth/microsoft/verify` must reject with `503 Service Unavailable` and JSON body `{"error":"Microsoft client-side login is disabled"}`
- rollout may turn these flags on only after **Release 3 / Phase C** is complete

## Backend component design

### Configuration

Add Microsoft configuration alongside Google in Spring Security and application configuration.

Expected backend configuration additions:

- Microsoft OAuth2 client registration for server-side login
- Microsoft client secret and client ID
- Microsoft audience/tenant configuration supporting both personal and organizational accounts under the documented identifier requirements
- Microsoft verifier configuration for client-side token validation
- Flyway configuration for staged schema rollout
- conditional registration/bean creation so backend startup succeeds when Microsoft settings are absent and both Microsoft flags are `false`

Google config remains intact.

### Provider-specific integration points

Keep provider-specific adapters at the edge:

- `GoogleTokenVerifier`
- `MicrosoftTokenVerifier`
- provider-specific OAuth2 claim extraction/mapping logic

Microsoft-specific registrations and beans must be created conditionally:

- create Microsoft `ClientRegistration`, verifier beans, and public provider-config output only when required Microsoft settings are present
- when Microsoft settings are absent and Microsoft flags are `false`, omit Microsoft registration/beans and expose Microsoft as hard-disabled via `/api/auth/providers`
- if either Microsoft flow flag is `true` while its required Microsoft configuration is missing, backend startup must fail fast with a clear configuration error

The success handler should stop assuming Google-specific fields directly. It should either:

- become provider-aware and delegate to a shared normalization service, or
- delegate the provider-specific claim extraction to adapters/services and only orchestrate success behavior

For Microsoft server-side login, default single-issuer validation against `common` is not acceptable. The implementation must use a custom OIDC user-service / ID-token-validation path that performs tenant-aware Microsoft v2.0 issuer validation using Microsoft metadata/JWKS while still allowing `common` as the interactive sign-in entry point.

For Microsoft server-side login, the Microsoft **ID token** is the authoritative source for `iss`, `sub`, and email extraction precedence used in normalization. UserInfo or Graph data may enrich optional fields such as display name or profile photo, but must never override authoritative identity claims.

Microsoft server-side registration must request at minimum the scopes `openid profile email`. If the resulting server-side ID token does not contain the claims required for the documented normalization contract, login must be rejected.

### Shared backend services

Refactor shared logic to be provider-aware:

- `UserService.findOrCreateUser(...)` becomes provider-aware and consumes normalized identity data
- `JwtTokenService` remains shared, but must carry provider-aware identity semantics in the token/profile contract
- `AuthCodeStore` and Redis exchange flow remain shared
- `/auth/exchange`, `/auth/logout`, and `/user/me` remain shared endpoints

### JWT and authenticated identity contract

Provider separation is a product requirement, so the authenticated identity contract must make it impossible for downstream logic to rely on email alone.

Required rules:

- JWT identity must remain unambiguous for provider-separated accounts
- JWT must carry authoritative provider-aware identity, not email alone
- JWT `sub` must be the internal `User.id`
- JWT must include `provider`
- JWT must include canonical `providerUserId` as an additional claim
- downstream authenticated logic must not treat email by itself as the canonical account key
- in steady state, the auth filter and `/user/me` resolution path must load the authenticated user from JWT `sub` rather than from email
- during Release 3 / Phases A and B only, the auth filter and `/user/me` must preserve a temporary dual-resolution bridge: legacy unversioned JWTs resolve by email subject, and `ver=2` JWTs resolve by `User.id`
- `/user/me` must expose `provider` and enough identity context for the UI and tests to distinguish provider-separated accounts when needed

Email may remain a display-oriented claim, but account identity must stay provider-aware end to end.

### API surface

Preserve existing Google endpoints and add Microsoft peers.

Existing endpoints:

- `GET /api/oauth2/authorization/google`
- `POST /api/auth/google/verify`
- `POST /api/auth/exchange`
- `POST /api/auth/logout`
- `GET /api/user/me`

New Microsoft endpoints:

- `GET /api/oauth2/authorization/microsoft`
- `POST /api/auth/microsoft/challenge`
- `POST /api/auth/microsoft/verify`
- `GET /api/auth/providers` for runtime provider availability and public client auth configuration

The frontend continues to use the shared code exchange and profile endpoints after either provider succeeds.

### Microsoft verify endpoint contract

`POST /api/auth/microsoft/challenge`

Response body:

```json
{
  "challengeId": "<single-use-id>",
  "nonce": "<nonce-to-send-to-msal>"
}
```

The backend stores the challenge as a short-lived single-use record. The challenge is consumed during Microsoft verify.

Challenge issuance controls:

- challenge responses must send `Cache-Control: no-store`
- challenges must use a short TTL
- challenge issuance creates or reuses a pre-auth **challenge-session** identified by an HttpOnly cookie
- the backend stores challenges by `{challengeSessionId, challengeId, nonce}` and `POST /api/auth/microsoft/verify` must require the matching challenge-session cookie
- the backend must enforce a hard limit of **at most 20 active Microsoft challenges per IP over a rolling 10-minute window**, regardless of session rotation
- the backend must also enforce **at most 5 active Microsoft challenges per {challengeSessionId, IP} over a rolling 10-minute window** as a secondary control
- excess challenge creation attempts must return `429 Too Many Requests` with `{"error":"Too many Microsoft login challenges"}`
- expired or consumed challenges no longer count toward the limit
- expired, replayed, duplicate, or already-consumed challenges must be rejected

Challenge-session cookie contract:

- cookie name: `ms_challenge_session`
- attributes: `HttpOnly`, `SameSite=Lax`, `Path=/api/auth/microsoft`, short TTL aligned to challenge lifetime
- `Secure=true` in HTTPS environments; local HTTP development may relax `Secure` only for localhost development
- the cookie is rotated/reissued when the backend creates a new anonymous challenge session
- frontend challenge and verify requests must use fetch credentials behavior compatible with this cookie contract so the same pre-auth session is sent on both calls

`POST /api/auth/microsoft/verify`

Request body:

```json
{
  "credential": "<microsoft-id-token>",
  "challengeId": "<single-use-id>"
}
```

Response contracts:

- success: `200 OK`

```json
{ "token": "<jwt>" }
```

- invalid Microsoft token, non-ID token, or failed validation: `400 Bad Request`

```json
{ "error": "Invalid Microsoft credential" }
```

- client-side Microsoft login disabled by feature flag: `503 Service Unavailable`

```json
{ "error": "Microsoft client-side login is disabled" }
```

### Runtime provider config contract

Because this is a static Vite frontend, Microsoft provider availability must come from the backend at runtime rather than duplicated build-time assumptions.

Required contract:

- `GET /api/auth/providers` is an unauthenticated JSON endpoint that returns provider availability and public browser-auth config
- the home page renders Microsoft options only from this backend payload
- the payload includes Microsoft `serverSideEnabled`, `clientSideEnabled`, and public MSAL config needed by the browser, such as `clientId`, `authority`, and scopes
- when Microsoft is disabled or not configured, the payload must still be valid and must represent Microsoft as hard-disabled
- when Microsoft is disabled in backend flags, the frontend must not render enabled Microsoft cards even if stale frontend config exists
- rollout must be **backend-first**: deploy `/api/auth/providers` support before shipping the frontend that depends on it
- if `/api/auth/providers` returns `404`, `5xx`, or a network error, the frontend must fall back to **Google unchanged, Microsoft disabled** rather than failing app bootstrap

Required response schema:

```json
{
  "google": {
    "serverSideEnabled": true,
    "clientSideEnabled": true
  },
  "microsoft": {
    "serverSideEnabled": false,
    "clientSideEnabled": false,
    "clientId": null,
    "authority": null,
    "scopes": []
  }
}
```

Freshness rules:

- responses must send `Cache-Control: no-store`
- frontend must fetch `/api/auth/providers` on every app load
- frontend must not persist this payload in local storage or any long-lived client cache
- backend startup must succeed when Microsoft configuration is absent as long as both Microsoft feature flags remain `false`
- frontend must treat `null` or missing Microsoft public config fields as hard-disabled and must not initialize MSAL in that state

### Frontend bootstrap order

- the app boots in a Google-capable baseline state
- frontend fetches `/api/auth/providers` before creating any Microsoft MSAL instance
- MSAL is initialized lazily only when `microsoft.clientSideEnabled=true` and valid public Microsoft config is present
- if runtime provider config is missing, disabled, or invalid, Microsoft client-side auth is never mounted and Google flows remain available

## Frontend design

### Home page layout

The home page should explicitly present four auth options:

- Google Server-Side
- Google Client-Side
- Microsoft Server-Side
- Microsoft Client-Side

This preserves the current teaching/demo style and mirrors the approved preference for provider/flow parity.

### Frontend structure

Keep the UI explicit, but allow small shared abstractions where they clearly reduce duplication without obscuring meaning.

Reasonable shared frontend pieces:

- auth card wrapper/presentation styles
- shared error messaging display
- shared post-login code exchange behavior
- provider availability bootstrap from `/api/auth/providers`
- lazy MSAL initialization only after runtime provider config confirms Microsoft client-side auth is enabled

Avoid over-abstracting the page into a generic provider engine. The page should remain easy to scan and understand.

### Auth state behavior

Keep existing auth state handling principles:

- JWT stored in `localStorage`
- `useAuth` remains the central place for auth state and profile fetches
- `/user/me` is still the source of authenticated user profile data
- unauthorized profile fetches still trigger logout/reset behavior
- server-side flow still uses `?code=` and the shared exchange endpoint

### Microsoft-specific UX notes

Treat profile photo as optional because Microsoft identity data may not always provide it in the same way as Google.

The home page and labels should be updated so the app is no longer described as Google-only.

When Microsoft is disabled via runtime provider config, the Microsoft cards must be hidden or rendered disabled consistently with the backend feature-flag state.

## Error handling

Use consistent user-facing behavior across providers.

Shared rules:

- invalid or expired auth exchange code → remain on home page and show feedback
- invalid provider token → show provider-specific error label/message through shared UI behavior
- missing required identity claims → reject login
- unverified email → reject login for providers that expose an explicit verification signal required by the app
- missing profile image → allowed
- unauthorized access to protected endpoints → same logout/reset behavior as current implementation

Microsoft-specific rule:

- Microsoft login acceptance is based on a valid Microsoft ID token plus an acceptable email-like claim under the documented precedence rules; it does not require a separate Google-style `email_verified` signal

Microsoft-specific claim differences should be handled inside normalization logic rather than leaking into the rest of the app.

## Security and identity rules

- Do not place JWTs directly in redirect URLs
- Continue using Redis-backed single-use code exchange for server-side OAuth success redirects
- Keep JWT-based stateless auth for protected API requests
- Keep account identity scoped to provider identity, not email
- Ensure provider-aware verification validates intended audience/issuer/tenant assumptions for Microsoft
- Preserve Google validation behavior after refactor

## Testing strategy

### Backend tests

Add or update tests for:

- Microsoft token verification behavior
- Microsoft personal-account success behavior
- Microsoft work/school-account success behavior for accounts that satisfy the documented identifier rule
- provider-aware `UserService` upsert logic
- same email + different provider results in separate users
- invalid Microsoft audience/issuer/tenant rejection
- rejection of Microsoft Graph access tokens and any other non-ID token at `/api/auth/microsoft/verify`
- rejection of missing required Microsoft identity claims
- Microsoft challenge issuance, nonce matching, and single-use challenge consumption
- `POST /api/auth/microsoft/challenge` returns the required `503` response when Microsoft client-side auth is disabled
- provider-aware OAuth2 success handling
- controller tests for Microsoft verify endpoint
- regression tests ensuring Google still works after the model refactor
- auth-code exchange behavior remains unchanged
- auth-code replay rejection after first successful exchange
- migration/backfill preservation of existing Google users
- same Microsoft account resolved through both server-side and client-side flows maps to the same `(provider, provider_user_id)` identity
- same Google account resolved through both flows still maps to the same provider-aware identity after refactor
- during cutover Phases A and B, both legacy and `v2` JWT/auth-code artifacts are accepted according to the compatibility rules
- in cutover Phase C, legacy JWTs using `sub = email` are rejected
- in cutover Phase C, pre-cutover legacy `authcode:*` payloads are rejected
- runtime provider config endpoint reflects enabled/disabled Microsoft flow state correctly
- disabled Microsoft endpoints return the required `503` status codes and error bodies
- direct Microsoft callback or mid-flight flag-disable cases fail closed without issuing JWTs or auth codes
- mixed-version auth-code exchange works during the compatibility window by redeeming legacy `authcode:*` and `authcode:v2:*` namespaces correctly
- Microsoft challenge issuance enforces TTL, no-store, replay rejection, and abuse limits
- JWT validator behavior is phase-aware for both legacy unversioned tokens and `ver=2` tokens
- `/api/auth/providers` responses include the required schema and `Cache-Control: no-store` behavior

### Frontend tests

Add or update tests for:

- four login cards rendering on the home page when all providers are enabled
- Microsoft server-side card links correctly
- Microsoft client-side flow wiring and error handling
- homepage provider rendering follows `/api/auth/providers` runtime config rather than duplicated frontend-only assumptions
- Microsoft cards are hidden or disabled when runtime provider config reports the corresponding flow disabled
- when `/api/auth/providers` is unavailable, the homepage still renders Google flows and suppresses Microsoft flows
- MSAL is initialized only after runtime provider config enables Microsoft client-side auth and provides valid public config
- existing route protection, logout, and token handling still work
- URL code exchange behavior still strips `?code=` and navigates correctly

### Integration/E2E tests

Cover at least:

- Google flow regression
- Microsoft server-side success path
- Microsoft client-side success path
- Microsoft personal-account authentication path
- Microsoft work/school-account authentication path for an account that satisfies the documented identifier rule
- distinct account creation for same email across Google and Microsoft
- same Microsoft account authenticated through both flows reuses the same stored identity
- Microsoft disabled-flag behavior matches the required backend status codes/error bodies and frontend hide/disable rules
- replay of a consumed Microsoft client-side challenge is rejected

### CI-safe testing split

- unit and integration tests use fixture tokens, mocked provider responses, or JWKS stubs rather than live Microsoft accounts
- Playwright E2E coverage in CI mocks browser/provider interactions where necessary to validate app behavior deterministically
- real personal-account and work/school-account sign-in runs as an opt-in manual or gated smoke suite with dedicated test accounts
- the cutover invalidation mechanism is tested explicitly: JWT `v2` enforcement plus Redis auth-code namespace rotation from legacy `authcode:*` to `authcode:v2:*`

## Implementation sequence

Recommended order:

1. **Release 1:** add Flyway, baseline the existing schema, switch non-test environments to `spring.jpa.hibernate.ddl-auto=validate`, add nullable provider columns, and deploy dual-write for legacy Google logins
2. **Release 2:** backfill provider columns for existing Google users, deterministically drop email uniqueness, add `(provider, provider_user_id)` uniqueness, keep Microsoft disabled, and preserve the temporary application-level invariant that no duplicate-email rows are created while legacy JWTs remain valid
3. **Release 3 / Phase A:** deploy backend/auth changes that support dual resolution for rolling compatibility: legacy unversioned JWTs resolve by email, `v2` JWTs resolve by `User.id`, and all nodes accept both artifact formats
4. **Release 3 / Phase B:** switch minting/storage to JWT `ver=2` and `authcode:v2:*` while all nodes still accept legacy artifacts during the compatibility window
5. **Release 3 / Phase C:** after the maximum legacy JWT/auth-code TTL expires, reject legacy artifacts, enforce `identityContractMode=V2_ONLY`, and keep Microsoft disabled if this invariant is not satisfied
6. Add Microsoft server-side OAuth2 registration, Microsoft client-side challenge/verify support, and `/api/auth/providers` runtime config behind disabled Microsoft feature flags
7. Ship the frontend changes that consume `/api/auth/providers`, lazily bootstrap Microsoft client auth, and render Microsoft cards only when runtime config enables them
8. Run the automated provider-aware, rollout, feature-flag, runtime-config, and regression test suites
9. Enable Microsoft feature flags only after Release 3 / Phase C and all rollout gate conditions are satisfied
10. Update environment examples and docs, then retire `google_id` only in a later cleanup release

## Risks and mitigations

### Risk: breaking the existing Google flow

**Mitigation:** add regression coverage before or during the provider-aware refactor and keep provider-specific adapters thin.

### Risk: schema transition issues from `google_id` to provider-aware identity

**Mitigation:** require Flyway-managed staged migrations, validate backfill results, and keep rollback steps explicit.

### Risk: Microsoft claim differences or optional profile attributes

**Mitigation:** centralize normalization, define exact email/issuer rules, and keep optional fields optional.

### Risk: over-abstraction in the frontend

**Mitigation:** keep explicit provider cards and only extract runtime-config and presentation helpers where they improve readability.

## Acceptance criteria

The design is considered satisfied when:

- Google server-side and client-side flows continue to work
- Microsoft server-side and client-side flows are both available behind controlled rollout flags
- Microsoft personal and work/school accounts that satisfy the documented email-like identifier rule are supported according to the chosen audience configuration
- the home page clearly presents four auth options when all providers are enabled
- same email across providers produces separate accounts
- backend no longer relies on Google-specific shared identity fields
- email is no longer used as a unique database identity key
- existing Google users remain login-compatible after the provider-aware migration
- Redis auth-code exchange remains the redirect-safe mechanism for server-side flows
- a reused auth code is rejected after the first successful exchange
- Microsoft tokens with invalid audience, issuer, or tenant assumptions are rejected
- Microsoft Graph access tokens and other non-ID tokens are rejected by the Microsoft verify endpoint
- missing required Microsoft identity claims cause login rejection
- authenticated identity remains provider-aware and is not keyed by email alone in shared backend logic
- runtime provider config accurately controls Microsoft UI availability
- tests cover Microsoft additions, rollout gates, and Google regressions

## Summary

This design adds Microsoft SSO with full parity while refactoring the backend just enough to support multiple providers cleanly. It preserves the secure redirect/code exchange pattern, makes provider identity explicit so Google and Microsoft accounts remain distinct even when emails match, and stages rollout through Flyway migrations, JWT/Redis cutover, runtime provider config, and backend/frontend feature gating.

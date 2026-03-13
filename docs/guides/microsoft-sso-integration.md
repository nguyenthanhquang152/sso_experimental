# Microsoft SSO Integration

## Overview

This project now supports Microsoft Single Sign-On alongside Google across both existing login styles:

- **Server-side authorization code flow** via Spring Security
- **Client-side ID token verification flow** from the React app

The homepage is now provider-driven at runtime through `GET /api/auth/providers`, so the frontend can show Google and Microsoft options without requiring a rebuild for feature-flag changes.

## What was added

### Backend

- Added Microsoft runtime configuration through `app.microsoft.*`
- Added `GET /api/auth/providers` metadata for both Google and Microsoft
- Added Microsoft client-side challenge/verify endpoints:
  - `POST /api/auth/microsoft/challenge`
  - `POST /api/auth/microsoft/verify`
- Added Microsoft ID token verification with validation for:
  - audience (`aud`)
  - issuer (`iss`)
  - tenant (`tid`)
  - nonce
  - token version `2.0`
- Added provider-neutral identity normalization so Google and Microsoft share the same persistence and JWT minting pipeline
- Added rollout/config guards so Microsoft cannot be advertised or enabled unless required public config is present
- Added Microsoft authorization gate handling for `/oauth2/authorization/microsoft`
- Added Flyway migration `V5__make_google_id_nullable.sql` to support non-Google identities cleanly

### Frontend

- Reworked the homepage into a provider-agnostic login screen (`SSO Demo`)
- Added runtime provider-config fetching via `useProviderConfig()`
- Added a reusable login card layout for auth options
- Added Microsoft server-side login card
- Added Microsoft client-side login button using `@azure/msal-browser`
- Switched Google client bootstrap to runtime config from `/api/auth/providers`
- Added a Vite `/api` dev proxy to `http://localhost:8080`
- Updated Playwright coverage for runtime provider rendering and logout behavior

## Runtime provider contract

`GET /api/auth/providers` returns the public auth metadata the frontend needs.

Example response:

```json
{
  "google": {
    "serverSideEnabled": true,
    "clientSideEnabled": true,
    "clientId": "google-client-id.apps.googleusercontent.com"
  },
  "microsoft": {
    "serverSideEnabled": true,
    "clientSideEnabled": true,
    "clientId": "microsoft-client-id",
    "authority": "https://login.microsoftonline.com/common/v2.0",
    "scopes": ["openid", "profile", "email"]
  }
}
```

Microsoft is hard-disabled in this payload if the feature flags are on but the public Microsoft configuration is incomplete.

## Microsoft flows

### Server-side flow

1. User clicks **Continue with Microsoft (Server-Side)**
2. Browser goes to `/api/oauth2/authorization/microsoft`
3. Spring Security performs the Microsoft authorization code flow
4. On success, the backend creates or updates the user, mints a JWT, stores it in the short-lived auth-code store, and redirects the SPA with `?code=...`
5. The frontend exchanges the code through `POST /api/auth/exchange`

### Client-side flow

1. Frontend calls `POST /api/auth/microsoft/challenge`
2. Backend returns `{ challengeId, nonce }` and sets a short-lived session cookie scoped to `/api/auth/microsoft`
3. Frontend opens Microsoft sign-in using MSAL popup with the issued nonce
4. Frontend sends the returned ID token to `POST /api/auth/microsoft/verify`
5. Backend consumes the single-use challenge, verifies the token, normalizes the identity, persists the user, and returns a JWT
6. Frontend stores the JWT and navigates to `/dashboard`

## Important validation rules

Microsoft client-side verification rejects tokens when:

- the audience does not match the configured Microsoft client ID
- the issuer does not match the expected tenant issuer format
- the `tid` claim is missing or inconsistent with the issuer
- the token version is not `2.0`
- the nonce does not match the issued challenge
- the identity looks like an unsupported guest/external account

## Key files

### Backend

- `backend/src/main/java/com/demo/sso/controller/AuthController.java`
- `backend/src/main/java/com/demo/sso/controller/ProviderConfigController.java`
- `backend/src/main/java/com/demo/sso/config/MicrosoftAuthorizationGateFilter.java`
- `backend/src/main/java/com/demo/sso/config/MicrosoftAuthProperties.java`
- `backend/src/main/java/com/demo/sso/config/IdentityContractGuard.java`
- `backend/src/main/java/com/demo/sso/service/MicrosoftTokenVerifier.java`
- `backend/src/main/java/com/demo/sso/service/ProviderIdentityNormalizer.java`
- `backend/src/main/java/com/demo/sso/service/MicrosoftChallengeStore.java`
- `backend/src/main/java/com/demo/sso/service/RedisMicrosoftChallengeStore.java`
- `backend/src/main/resources/db/migration/V5__make_google_id_nullable.sql`

### Frontend

- `frontend/src/App.tsx`
- `frontend/src/hooks/useProviderConfig.ts`
- `frontend/src/pages/HomePage.tsx`
- `frontend/src/components/LoginCard.tsx`
- `frontend/src/components/ClientSideLogin.tsx`
- `frontend/src/components/MicrosoftClientSideLogin.tsx`
- `frontend/src/components/ServerSideLogin.tsx`
- `frontend/src/types/auth.ts`
- `frontend/vite.config.ts`

## Configuration

### Backend environment variables

| Variable | Required for Microsoft | Description |
|---|---|---|
| `MICROSOFT_CLIENT_ID` | Yes | Public Microsoft app/client ID |
| `MICROSOFT_CLIENT_SECRET` | Server-side only | Microsoft client secret for Spring Security OAuth2 login |
| `MICROSOFT_AUTHORITY` | No | Defaults to `https://login.microsoftonline.com/common/v2.0` |
| `MICROSOFT_SCOPES` | No | Defaults to `openid,profile,email` |
| `APP_AUTH_MICROSOFT_SERVER_SIDE_ENABLED` | No | Enables Microsoft server-side flow |
| `APP_AUTH_MICROSOFT_CLIENT_SIDE_ENABLED` | No | Enables Microsoft client-side flow |

### Existing auth prerequisites still required

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `JWT_SECRET`

## Verification completed

### Backend

Verified with focused Maven tests covering:

- `ProviderConfigControllerTest`
- `MicrosoftClientSideIntegrationTest`

Result:

- `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`
- `BUILD SUCCESS`

### Frontend

Verified with:

- `npm run lint`
- `npm run build`
- `CI=1 PLAYWRIGHT_HTML_OPEN=never npx playwright test --reporter=list`

Result:

- `10 passed, 4 skipped`
- skipped tests are backend-dependent API smoke checks that require `PLAYWRIGHT_API_BASE_URL` to target a running backend service

## Notes

- The frontend bootstraps Google from runtime provider config and only falls back to build-time `VITE_GOOGLE_CLIENT_ID` when it is explicitly provided for local development.
- The Vite dev server now proxies `/api` requests to `http://localhost:8080` for local full-stack development.
- Microsoft client-side login uses a backend-issued, single-use challenge to bind the popup ID token to the current browser session.

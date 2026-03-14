# Google SSO Integration

## Overview

This repository supports Google sign-in through two flows:

- **Server-side OAuth2 flow** — Spring Security handles the Google authorization-code exchange and redirects the browser back with a one-time auth code.
- **Client-side credential flow** — the browser receives a Google credential directly, the backend verifies it, and then issues the application JWT.

In both flows, the frontend ultimately receives the same application JWT and stores it in `localStorage` for authenticated API calls.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  Browser (React 19 + Vite)                              │
│  http://localhost:8000                                  │
└──────────────┬──────────────────────────────────────────┘
               │
        ┌──────▼──────┐
        │   Traefik   │  :8000
        │   (v3.6)    │
        └──┬──────┬───┘
           │      │
     ──────▼──    ▼──────────────
    Priority 1   Priority 2
    Host(*)      Host(*) && PathPrefix(/api)
           │      │
    ┌──────▼──┐  ┌▼───────────┐
    │Frontend │  │  Backend   │
    │ (nginx) │  │Spring Boot │
    │  :80    │  │   :8080    │
    └─────────┘  └─────┬──┬──┘
                       │  │
                 ┌─────▼┐ ┌▼──────────┐
                 │Postgres│ │   Redis   │
                 │  :5432 │ │   :6379   │
                 └────────┘ └───────────┘
```

Traefik routes all requests on port `8000`. Requests with path prefix `/api` go to the Spring Boot backend; everything else serves the React SPA. The backend uses PostgreSQL for user persistence and Redis for short-lived authorization codes.

## OAuth2 Flows

### Flow 1: Server-Side Authorization Code Flow

The browser never touches Google tokens directly. Spring Security manages the entire handshake.

```
Browser                     Backend (Spring Security)            Google
  │                                │                               │
  │ 1. Click "Sign in with        │                               │
  │    Google (Server-Side)"      │                               │
  │ ──────────────────────────▶   │                               │
  │    GET /api/oauth2/           │                               │
  │    authorization/google       │                               │
  │                                │ 2. Redirect to Google         │
  │ ◀──────────────────────────── │    with client_id, scopes,    │
  │    302 → Google OAuth consent │    redirect_uri               │
  │                                │                               │
  │ ──────────────────────────────────────────────────────────▶   │
  │ 3. User authenticates & consents                              │
  │ ◀──────────────────────────────────────────────────────────── │
  │    302 → /api/login/oauth2/code/google?code=AUTH_CODE         │
  │                                │                               │
  │ ──────────────────────────▶   │ 4. Spring exchanges code      │
  │                                │    for tokens with Google     │
  │                                │ ─────────────────────────▶   │
  │                                │ ◀───────────────────────────  │
  │                                │    access_token + id_token    │
  │                                │                               │
  │                                │ 5. OAuth2SuccessHandler:      │
  │                                │    - Extract user attrs       │
  │                                │    - Verify email             │
  │                                │    - findOrCreateUser()       │
  │                                │    - Generate JWT             │
  │                                │    - Store JWT in Redis       │
  │                                │      with 30s auth code       │
  │                                │                               │
  │ ◀──────────────────────────── │ 6. 302 → /?code=AUTH_CODE     │
  │                                │                               │
  │ 7. HomePage detects ?code=    │                               │
  │    POST /api/auth/exchange    │                               │
  │    { code: AUTH_CODE }        │                               │
  │ ──────────────────────────▶   │ 8. Redis: get-and-delete      │
  │ ◀──────────────────────────── │    Returns JWT                │
  │    { token: JWT }             │                               │
  │                                │                               │
  │ 9. Store JWT in localStorage  │                               │
  │    Navigate to /dashboard     │                               │
```

**Key classes:**
- `ServerSideLogin` (`frontend/src/components/ServerSideLogin.tsx`) — renders the link to `/api/oauth2/authorization/google`
- `SecurityConfig.filterChain()` (`backend/.../config/SecurityConfig.java`) — configures `oauth2Login()` with the success handler
- `OAuth2SuccessHandler.onAuthenticationSuccess()` (`backend/.../service/OAuth2SuccessHandler.java`) — persists user, generates JWT, stores auth code in Redis, redirects to frontend
- `RedisAuthCodeStore` (`backend/.../service/RedisAuthCodeStore.java`) — stores JWT→code mapping in Redis with 30s TTL, single-use via `GETDEL`
- `AuthController.exchangeCode()` (`backend/.../controller/AuthController.java`) — exchanges code for JWT
- `HomePage` (`frontend/src/pages/HomePage.tsx`) — detects `?code=` param and calls `/auth/exchange`

### Flow 2: Client-Side Flow

The browser obtains a Google ID token directly via popup, then sends it to the backend for verification.

```
Browser                     Backend                        Google
  │                            │                              │
  │ 1. Click Google Sign-In   │                              │
  │    (popup via @react-     │                              │
  │    oauth/google)          │                              │
  │ ─────────────────────────────────────────────────────▶  │
  │ 2. User authenticates in Google popup                    │
  │ ◀────────────────────────────────────────────────────── │
  │    credential (Google ID token)                          │
  │                            │                              │
  │ 3. POST /api/auth/google/ │                              │
  │    verify                  │                              │
  │    { credential: ID_TOKEN }│                              │
  │ ───────────────────────▶  │ 4. GoogleTokenVerifier:      │
  │                            │    verify ID token with      │
  │                            │    Google's public keys      │
  │                            │    Check audience matches    │
  │                            │    client ID                 │
  │                            │                              │
  │                            │ 5. Extract payload:          │
  │                            │    sub, email, name, picture │
  │                            │    Verify email_verified     │
  │                            │    findOrCreateUser()        │
  │                            │    Generate JWT              │
  │                            │                              │
  │ ◀───────────────────────  │                              │
  │    { token: JWT }          │                              │
  │                            │                              │
  │ 6. Store JWT in           │                              │
  │    localStorage            │                              │
  │    Navigate to /dashboard  │                              │
```

**Key classes:**
- `ClientSideLogin` (`frontend/src/components/ClientSideLogin.tsx`) — uses `@react-oauth/google` `GoogleLogin` component, sends credential to backend
- `AuthController.verifyGoogleToken()` (`backend/.../controller/AuthController.java`) — receives credential, verifies via `GoogleTokenVerifier`, creates user, returns JWT
- `GoogleTokenVerifier.verify()` (`backend/.../service/GoogleTokenVerifier.java`) — validates the Google ID token using `google-api-client`, checks audience

## Key Implementation Details

### Security Configuration

**File:** `backend/src/main/java/com/demo/sso/config/SecurityConfig.java`

- **Stateless sessions** — `SessionCreationPolicy.STATELESS`; no server-side HTTP sessions
- **CSRF disabled** for `/auth/**` and `/user/**` endpoints (API-only paths)
- **Public paths:** `/oauth2/**`, `/login/oauth2/**`, `/auth/**`
- **Protected paths:** `/user/**` and all others require authentication
- **JWT filter** (`JwtAuthenticationFilter`) runs before `UsernamePasswordAuthenticationFilter`
- **OAuth2 login** configured with `OAuth2SuccessHandler`
- **401 handler** returns JSON `{"error":"Unauthorized"}` instead of redirecting

### JWT Authentication Filter

**File:** `backend/src/main/java/com/demo/sso/config/JwtAuthenticationFilter.java`

Extends `OncePerRequestFilter`. Extracts the `Authorization: Bearer <token>` header, validates the JWT via `JwtTokenService.isTokenValid()`, and sets a `UsernamePasswordAuthenticationToken` in `SecurityContextHolder` with the user's email as principal and `ROLE_USER` authority.

### JWT Token Service

**File:** `backend/src/main/java/com/demo/sso/service/JwtTokenService.java`

Uses **jjwt 0.12.6** (HMAC-SHA). Key behaviors:

| Method | Description |
|---|---|
| `generateToken(email, googleId)` | Creates JWT with `sub=email`, `googleId` claim, `iss=sso-demo-backend`, `aud=sso-demo-api`, random `jti`, and configurable expiration (default 24h) |
| `parseToken(token)` | Parses and validates signature, issuer, audience; returns `Claims` |
| `getEmailFromToken(token)` | Extracts the `sub` claim |
| `isTokenValid(token)` | Returns `true` if parsing succeeds, `false` for any `JwtException` |

The constructor validates the secret: minimum 32 characters, rejects placeholder values like `"default"` or `"change-me"`.

### Google Token Verifier

**File:** `backend/src/main/java/com/demo/sso/service/GoogleTokenVerifier.java`

Wraps Google's `GoogleIdTokenVerifier` from `google-api-client 2.7.2`. Configured with the app's Google Client ID as the expected audience. The `verify()` method returns the token payload or throws `IllegalArgumentException` if invalid.

### OAuth2 Success Handler

**File:** `backend/src/main/java/com/demo/sso/service/OAuth2SuccessHandler.java`

Implements `AuthenticationSuccessHandler`. Called by Spring Security after a successful server-side OAuth2 login:

1. Extracts `email_verified`, `sub`, `email`, `name`, `picture` from `OAuth2User`
2. Rejects login if email is not verified (redirects with `?error=email_not_verified`)
3. Rejects login if `sub` or `email` is missing (redirects with `?error=missing_attributes`)
4. Calls `UserService.findOrCreateUser()` with login method `"SERVER_SIDE"`
5. Generates a JWT via `JwtTokenService`
6. Stores JWT in Redis via `AuthCodeStore.storeJwt()` and gets a single-use code
7. Redirects to `{frontendUrl}/?code={code}`

### Auth Code Store (Redis)

**File:** `backend/src/main/java/com/demo/sso/service/RedisAuthCodeStore.java`

Implements the `AuthCodeStore` interface. This avoids passing JWTs via URL query parameters:

- `storeJwt(jwt)` — generates a 32-byte random code (URL-safe Base64), stores in Redis with key prefix `authcode:` and 30-second TTL
- `exchangeCode(code)` — uses Redis `GETDEL` for atomic get-and-delete, ensuring single use

A `@Profile("!test")` annotation excludes it from test context; tests use `InMemoryAuthCodeStore` instead.

### User Service

**File:** `backend/src/main/java/com/demo/sso/service/UserService.java`

- `findOrCreateUser()` — looks up user by `googleId`; if found, updates name/picture/loginMethod/lastLoginAt; if not, creates a new user. Handles concurrent creation via catch on `DataIntegrityViolationException`
- `findByEmail()` — used by `UserController` to fetch the authenticated user's profile

### Frontend Auth Hook

**File:** `frontend/src/hooks/useAuth.ts`

Custom React hook managing auth state:

- Initializes `token` from `localStorage.getItem('jwt')`
- `login(jwt)` — stores token in `localStorage`, triggers profile fetch
- `logout()` — removes token from `localStorage`, clears user state
- Auto-fetches user profile from `/user/me` when token changes
- On 401 response, automatically calls `logout()`
- Exposes `{ token, user, loading, login, logout, isAuthenticated }`

### API Client

**File:** `frontend/src/api/client.ts`

`apiFetch<T>(path, options)` — wrapper around `fetch()` that:
- Prepends `/api` base path (Traefik routes to backend)
- Attaches `Authorization: Bearer <jwt>` header from `localStorage`
- Sets `Content-Type: application/json`
- Throws on non-OK responses

### Frontend bootstrap

**File:** `frontend/src/App.tsx`

The app fetches runtime provider metadata from `GET /api/auth/providers` before deciding whether to wrap routes with `GoogleOAuthProvider`. The Google client ID now comes from the backend-published provider contract first, with `VITE_GOOGLE_CLIENT_ID` used only as a fallback in local development.

## API Endpoints

All backend endpoints are prefixed with `/api` via `server.servlet.context-path`. Traefik strips nothing — the backend sees `/api/...` paths.

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/api/oauth2/authorization/google` | No | Initiates server-side OAuth2 flow (Spring Security built-in) |
| `GET` | `/api/login/oauth2/code/google` | No | OAuth2 callback (Spring Security built-in) |
| `POST` | `/api/auth/google/verify` | No | Client-side flow: verifies Google ID token, returns JWT |
| `POST` | `/api/auth/exchange` | No | Exchanges single-use auth code for JWT (server-side flow) |
| `POST` | `/api/auth/logout` | No | Returns logout confirmation message |
| `GET` | `/api/user/me` | Yes (Bearer JWT) | Returns authenticated user's profile |

### Request/Response Examples

**POST /api/auth/google/verify**
```json
// Request
{ "credential": "<Google ID token>" }
// Response 200
{ "token": "<JWT>" }
// Response 400
{ "error": "Invalid Google credential" }
```

**POST /api/auth/exchange**
```json
// Request
{ "code": "<single-use auth code>" }
// Response 200
{ "token": "<JWT>" }
// Response 400
{ "error": "Invalid or expired code" }
```

**GET /api/user/me** (with `Authorization: Bearer <JWT>`)
```json
// Response 200
{
  "id": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "pictureUrl": "https://lh3.googleusercontent.com/...",
  "loginMethod": "SERVER_SIDE",
  "createdAt": "2025-01-15T10:30:00Z",
  "lastLoginAt": "2025-01-15T12:00:00Z"
}
```

## Database Schema

A single `users` table managed by Flyway migrations and validated by Hibernate at startup (`spring.jpa.hibernate.ddl-auto: validate`):

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` | PK, auto-increment | Internal user ID |
| `google_id` | `VARCHAR` | UNIQUE, NOT NULL | Google subject identifier (`sub`) |
| `email` | `VARCHAR` | UNIQUE, NOT NULL | User's Google email |
| `name` | `VARCHAR` | nullable | Display name |
| `picture_url` | `VARCHAR` | nullable | Google profile picture URL |
| `login_method` | `VARCHAR(20)` | nullable | `SERVER_SIDE` or `CLIENT_SIDE` |
| `created_at` | `TIMESTAMP` | nullable | Auto-set on creation via `@PrePersist` |
| `last_login_at` | `TIMESTAMP` | nullable | Updated on each login |

**Entity:** `backend/src/main/java/com/demo/sso/model/User.java`
**Repository:** `backend/src/main/java/com/demo/sso/repository/UserRepository.java` — queries: `findByGoogleId()`, `findByEmail()`

## Configuration

### Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GOOGLE_CLIENT_ID` | Yes | — | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | Yes | — | Google OAuth2 client secret |
| `JWT_SECRET` | Yes | — | HMAC signing key (≥ 32 chars, not a placeholder) |
| `POSTGRES_DB` | No | `sso_demo` | PostgreSQL database name |
| `POSTGRES_USER` | No | `sso_user` | PostgreSQL username |
| `POSTGRES_PASSWORD` | No | `sso_pass` | PostgreSQL password |
| `FRONTEND_URL` | No | `http://localhost:8000` | Redirect target after OAuth2 success |
| `DOCKER_SOCK` | No | `/run/user/1000/docker.sock` | Docker socket path for Traefik |
| `VITE_GOOGLE_CLIENT_ID` | Optional fallback | — | Development fallback when backend provider metadata is unavailable |

### Application Config

**File:** `backend/src/main/resources/application.yml`

Key settings:
- Backend listens on port `8080` with context path `/api`
- `server.forward-headers-strategy: framework` — trusts Traefik's `X-Forwarded-*` headers for correct redirect URIs
- `spring.jpa.hibernate.ddl-auto: validate` — validates the Flyway-managed schema
- `app.jwt.expiration-ms: 86400000` — JWT expires after 24 hours
- OAuth2 client registered under `spring.security.oauth2.client.registration.google` with scopes `openid, profile, email`

## How to Run

### Prerequisites

- Docker and Docker Compose
- A Google Cloud project with OAuth2 credentials ([console.cloud.google.com](https://console.cloud.google.com))
  - Authorized redirect URI: `http://localhost:8000/api/login/oauth2/code/google`
  - Authorized JavaScript origin: `http://localhost:8000`

### Steps

1. **Copy and configure environment:**
   ```bash
   cp .env.example .env
   # Edit .env with your Google client ID, secret, and a strong JWT secret
   ```

2. **Optional fallback client ID for isolated frontend work:**
  ```bash
  cp frontend/.env.example frontend/.env
  # Set VITE_GOOGLE_CLIENT_ID only if you need the browser Google provider without backend-published config
  ```

3. **Start all services:**
   ```bash
   docker compose up --build
   ```

4. **Access the app:** Open [http://localhost:8000](http://localhost:8000)

### Stopping

```bash
docker compose down          # Stop containers
docker compose down -v       # Stop and remove database volume
```

## Testing

### Backend Unit & Integration Tests

Uses JUnit 5 with Spring Boot Test, MockMvc, Mockito, and H2 in-memory database. Redis is excluded via `@Profile("!test")` and replaced with `InMemoryAuthCodeStore` (`TestAuthCodeStoreConfig`).

```bash
cd backend
./mvnw test
```

**Test files:**

| File | What it tests |
|---|---|
| `service/JwtTokenServiceTest.java` | Token generation, parsing, validation, expiry, secret validation |
| `service/UserServiceTest.java` | User creation, update on re-login, email lookup |
| `service/OAuth2SuccessHandlerTest.java` | Attribute extraction, redirect with code, email verification rejection |
| `service/RedisAuthCodeStoreTest.java` | Redis store/exchange, TTL, single-use codes |
| `controller/ControllerIntegrationTest.java` | Full MockMvc integration: auth endpoints, JWT-protected `/user/me`, security filter chain |

### Frontend E2E Tests

Uses Playwright against the running application at `http://localhost:8000`.

```bash
cd frontend
npx playwright install    # First time only
npm run test:e2e          # Run tests (requires app running via docker compose)
npm run test:e2e:headed   # Run with browser visible
npm run test:e2e:report   # View HTML report
```

**Test files:**

| File | What it tests |
|---|---|
| `tests/e2e/homepage.spec.ts` | Page title, both login cards rendered, server-side link href |
| `tests/e2e/protected-route.spec.ts` | Unauthenticated redirect from `/dashboard` to `/` |
| `tests/e2e/logout.spec.ts` | localStorage cleared on logout, redirect after invalid token |
| `tests/e2e/token-handling.spec.ts` | Auth code exchange, JWT stored, code stripped from URL |
| `tests/e2e/api-health.spec.ts` | API endpoint smoke tests (logout 200, `/user/me` 401) |

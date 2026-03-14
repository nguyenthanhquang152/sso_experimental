# SSO Experimental

An experimental single sign-on playground that compares Google and Microsoft sign-in flows across a Spring Boot backend and a React frontend.

## What is in this repo?

- `backend/` — Spring Boot 3.5 API for OAuth, JWT minting, user persistence, rollout flags, and auth-code exchange
- `frontend/` — React 19 + Vite UI that renders provider cards from runtime config served by the backend
- `docs/` — guides, specs, plans, and research artifacts organized by document type
- `docker-compose.yml` — local stack with Traefik, frontend, backend, Postgres, and Redis

## Supported sign-in flows

### Google

- **Server-side flow** — browser redirects through Spring Security OAuth2
- **Client-side flow** — browser gets a Google credential, backend verifies it, then issues a JWT

### Microsoft

- **Server-side flow** — browser redirects through the backend OAuth2 client
- **Client-side flow** — frontend obtains a Microsoft ID token, backend verifies it using a challenge/nonce exchange

Provider availability is runtime-driven through `GET /api/auth/providers`, so the UI only renders flows that the backend currently exposes.

## Rollout flags

The backend exposes runtime feature flags under the `app.auth` config prefix. These control which authentication providers and flows are active without requiring a rebuild.

| Flag | Default | Purpose |
|---|---|---|
| `APP_AUTH_IDENTITY_CONTRACT_MODE` | `LEGACY_ONLY` | Controls identity contract format: `LEGACY_ONLY`, `COMPATIBILITY`, or `V2_ONLY` |
| `APP_AUTH_JWT_MINT_MODE` | `legacy` | Controls JWT token format: `legacy` or `v2` |
| `APP_AUTH_MICROSOFT_SERVER_SIDE_ENABLED` | `false` | Enables Microsoft server-side OAuth2 flow |
| `APP_AUTH_MICROSOFT_CLIENT_SIDE_ENABLED` | `false` | Enables Microsoft client-side MSAL flow |

Microsoft SSO requires `APP_AUTH_IDENTITY_CONTRACT_MODE=V2_ONLY`. The `GET /api/auth/providers` endpoint reflects which flows are currently enabled, so the frontend renders only the available options.

## Local development

### Prerequisites

- Docker / Docker Compose
- Java 25+
- Node.js 18+ and npm
- Maven

### Environment variables

Copy `.env.example` to `.env` and fill in the values you need. The full set of variables used by `docker-compose.yml`:

| Variable | Required | Default | Description |
|---|---|---|---|
| `GOOGLE_CLIENT_ID` | Yes (for Google) | — | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | Yes (for Google) | — | Google OAuth2 client secret |
| `JWT_SECRET` | Yes | — | Secret for signing application JWTs (≥ 32 chars) |
| `POSTGRES_DB` | No | `sso_demo` | PostgreSQL database name |
| `POSTGRES_USER` | No | `sso_user` | PostgreSQL username |
| `POSTGRES_PASSWORD` | No | `sso_pass` | PostgreSQL password |
| `MICROSOFT_CLIENT_ID` | Yes (for Microsoft) | — | Microsoft app/client ID |
| `MICROSOFT_CLIENT_SECRET` | Server-side only | — | Microsoft client secret |
| `MICROSOFT_AUTHORITY` | No | `https://login.microsoftonline.com/common/v2.0` | Microsoft authority URL |
| `MICROSOFT_SCOPES` | No | `openid,profile,email` | Microsoft OAuth2 scopes |
| `APP_AUTH_IDENTITY_CONTRACT_MODE` | No | `LEGACY_ONLY` | Identity contract mode (see Rollout flags) |
| `APP_AUTH_JWT_MINT_MODE` | No | `legacy` | JWT mint mode (see Rollout flags) |
| `APP_AUTH_MICROSOFT_SERVER_SIDE_ENABLED` | No | `false` | Enable Microsoft server-side flow |
| `APP_AUTH_MICROSOFT_CLIENT_SIDE_ENABLED` | No | `false` | Enable Microsoft client-side flow |

### Start the full stack

```bash
docker compose up --build
```

The application is exposed at `http://localhost:8000`.

## Verification commands

### Backend

```bash
cd backend
mvn -Dmaven.repo.local=../.m2/repository test
```

### Frontend

```bash
cd frontend
npm run lint
npm run build
```

### End-to-end

```bash
cd frontend
PLAYWRIGHT_API_BASE_URL=http://localhost:8000 npm run test:e2e
```

## Key runtime contracts

- `GET /api/auth/providers` publishes which Google and Microsoft flows are enabled
- `POST /api/auth/google/verify` verifies a Google browser credential and returns a JWT
- `POST /api/auth/microsoft/challenge` issues a challenge/nonce for Microsoft client-side sign-in
- `POST /api/auth/microsoft/verify` verifies a Microsoft ID token and returns a JWT
- `POST /api/auth/exchange` exchanges a one-time auth code for a JWT after server-side OAuth redirects
- `GET /api/user/me` returns the current authenticated user profile

## Docs

The `docs/` tree is organized by document type:

- `docs/guides/` — integration and usage guides
- `docs/specs/` — design specs
- `docs/plans/` — implementation plans
- `docs/research/` — dated investigations and experiments

Start with:

- `docs/guides/google-sso-integration.md`
- `docs/guides/microsoft-sso-integration.md`
- `docs/README.md`


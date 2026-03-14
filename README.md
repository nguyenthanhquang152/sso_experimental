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

## Local development

### Prerequisites

- Docker / Docker Compose
- Java 25+
- Node.js + npm
- Maven

### Start the full stack

1. Copy `.env.example` to `.env` and fill in the Google / Microsoft / JWT values you need.
2. Start the local stack:

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


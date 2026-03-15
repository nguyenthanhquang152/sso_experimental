# SSO Experimental

A full-stack Single Sign-On demo application supporting Google and Microsoft authentication via both server-side and client-side flows.

## Architecture

| Layer | Technology |
|-------|-----------|
| Backend | Java 25, Spring Boot 3.5, Spring Security, JPA |
| Frontend | React 19, TypeScript 5.9, Vite 7 |
| Database | PostgreSQL |
| Cache | Redis |
| Proxy | Traefik |
| E2E Tests | Playwright |

## Project Structure

```
sso_experimental/
├── backend/              # Spring Boot REST API
│   └── src/main/java/com/demo/sso/
│       ├── config/       # Security, JWT filters, exception handler
│       ├── controller/   # Auth, User, ProviderConfig endpoints
│       │   └── dto/      # Response DTOs (UserResponse, ErrorResponse)
│       ├── model/        # JPA entities
│       └── service/      # Business logic, JWT, OAuth
├── frontend/             # Vite + React SPA
│   └── src/
│       ├── api/          # API client with typed errors
│       ├── components/   # LoginCard, ClientSideLogin, etc.
│       ├── hooks/        # useAuth, useProviderConfig
│       ├── pages/        # HomePage, DashboardPage
│       └── types/        # TypeScript interfaces
├── docs/                 # Architecture documentation
└── docker-compose.yml    # Full stack orchestration
```

## Quick Start

### Prerequisites

- Java 25+
- Node.js 20+
- Docker & Docker Compose

### Development

1. **Start infrastructure** (PostgreSQL, Redis, Traefik):
   ```bash
   docker compose up -d postgres redis traefik
   ```

2. **Start backend**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

3. **Start frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. **Access the app** at `http://localhost:5173`

### Docker Compose (full stack)

```bash
docker compose up
```

Access via Traefik at `http://localhost:8000`

## Authentication Flows

| Provider | Flow | Description |
|----------|------|-------------|
| Google | Server-Side | Spring Security OAuth2 authorization code flow |
| Google | Client-Side | Google Identity Services → backend token verification |
| Microsoft | Server-Side | Microsoft Entra ID authorization code flow |
| Microsoft | Client-Side | MSAL.js → backend token verification |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/auth/google/verify` | Verify Google ID token (client-side flow) |
| `POST` | `/api/auth/microsoft/verify` | Verify Microsoft token (client-side flow) |
| `POST` | `/api/auth/exchange` | Exchange authorization code for JWT |
| `POST` | `/api/auth/logout` | Logout and invalidate session |
| `GET`  | `/api/user/me` | Get current user profile |
| `GET`  | `/api/auth/providers` | Get enabled provider configuration |
| `GET`  | `/api/oauth2/authorization/google` | Initiate Google server-side flow |
| `GET`  | `/api/oauth2/authorization/microsoft` | Initiate Microsoft server-side flow |

## Testing

```bash
# Preferred repo-local flow
just up
just check

# Backend unit + integration tests
cd backend && mvn test

# Frontend lint + type check
cd frontend && npm run build && npm run lint

# E2E tests (requires running app)
cd frontend && npx playwright test
```

`just check` includes Playwright E2E against `http://localhost:8000`. If the local stack is not running, the task runner fails fast and tells you to run `just up` first.

## Configuration

Key environment variables (see `backend/src/main/resources/application.yml`):

| Variable | Description |
|----------|-------------|
| `GOOGLE_CLIENT_ID` | Google OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret |
| `MICROSOFT_CLIENT_ID` | Microsoft Entra app client ID |
| `MICROSOFT_CLIENT_SECRET` | Microsoft Entra app secret |
| `JWT_SECRET` | Secret key for signing JWTs |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL |
| `SPRING_DATA_REDIS_HOST` | Redis host |

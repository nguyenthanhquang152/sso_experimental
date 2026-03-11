# Google SSO Integration Demo — Design Document

**Date:** 2026-03-11
**Status:** Approved

## Overview

A demo application showing Google SSO integration using two OAuth2 flows side-by-side:
1. **Server-Side Authorization Code Flow** — Spring Security handles the full OAuth2 dance
2. **Client-Side Flow** — React gets a Google ID token directly, backend verifies it

## Stack

| Component | Version | Purpose |
|---|---|---|
| React | 19.2.4 | Frontend SPA |
| Vite | 7.3.1 | Frontend build tool |
| @react-oauth/google | 0.13.4 | Client-side Google Sign-In |
| Spring Boot | 3.5.11 | Backend API |
| Spring Security | 6.x | OAuth2 client + JWT |
| PostgreSQL | 18.3 | User persistence |
| Traefik | 3.6.x | Reverse proxy |
| Java | 17+ | Runtime |

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Browser                           │
│  http://sso.localhost/           (React SPA)        │
│  http://sso.localhost/api/       (Spring Boot API)  │
└──────────────────────┬──────────────────────────────┘
                       │ :80
              ┌────────┴────────┐
              │    Traefik      │
              │   (port 80)     │
              └───┬─────────┬───┘
        path /    │         │  path /api/
    ┌─────────────┴──┐  ┌───┴──────────────┐
    │   React (Vite) │  │  Spring Boot     │
    │   :5173        │  │  :8080           │
    └────────────────┘  └────────┬─────────┘
                                 │
                        ┌────────┴─────────┐
                        │  PostgreSQL 18.3 │
                        │  :5432           │
                        └──────────────────┘
```

**Routing:** Path-based via Traefik. Single domain `sso.localhost`.

**Google Console:**
- Authorised JavaScript origins: `http://sso.localhost`
- Authorised redirect URIs: `http://sso.localhost/api/login/oauth2/code/google`

## OAuth2 Flows

### Flow 1: Server-Side Authorization Code

1. User clicks "Server-Side Login" on React
2. Browser navigates to `sso.localhost/api/oauth2/authorization/google`
3. Spring Security redirects to Google consent screen
4. Google redirects back to `sso.localhost/api/login/oauth2/code/google?code=xxx`
5. Spring Security exchanges code for tokens, loads user info
6. Backend saves/updates user in PostgreSQL
7. Backend generates JWT, redirects to `sso.localhost/?token=jwt`
8. React stores JWT, navigates to dashboard

### Flow 2: Client-Side (Google Identity Services)

1. User clicks "Client-Side Login" on React
2. @react-oauth/google shows Google Sign-In popup
3. Google returns credential (ID token) to React
4. React POSTs `{ credential }` to `sso.localhost/api/auth/google/verify`
5. Backend verifies ID token with Google
6. Backend saves/updates user in PostgreSQL
7. Backend returns `{ jwt }` to React
8. React stores JWT, navigates to dashboard

## REST API

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/oauth2/authorization/google` | No | Spring Security built-in — initiates server-side OAuth2 |
| GET | `/api/login/oauth2/code/google` | No | Spring Security built-in — Google callback |
| POST | `/api/auth/google/verify` | No | Custom — verifies client-side Google ID token |
| GET | `/api/user/me` | JWT | Returns current user profile |
| POST | `/api/auth/logout` | JWT | Invalidates session |

## Database Schema

```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    google_id       VARCHAR(255) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    name            VARCHAR(255),
    picture_url     TEXT,
    login_method    VARCHAR(20),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Project Structure

### Backend

```
backend/
├── src/main/java/com/demo/sso/
│   ├── SsoApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── JwtConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── UserController.java
│   ├── model/
│   │   └── User.java
│   ├── repository/
│   │   └── UserRepository.java
│   └── service/
│       ├── GoogleTokenVerifier.java
│       ├── UserService.java
│       └── OAuth2SuccessHandler.java
├── src/main/resources/
│   └── application.yml
├── Dockerfile
└── pom.xml
```

### Frontend

```
frontend/
├── src/
│   ├── App.tsx
│   ├── main.tsx
│   ├── pages/
│   │   ├── HomePage.tsx
│   │   └── DashboardPage.tsx
│   ├── components/
│   │   ├── ServerSideLogin.tsx
│   │   └── ClientSideLogin.tsx
│   ├── hooks/
│   │   └── useAuth.ts
│   └── api/
│       └── client.ts
├── index.html
├── Dockerfile
├── package.json
└── vite.config.ts
```

## Frontend Pages

**Home Page (`/`):**
- Title: "Google SSO Demo"
- Two cards: Server-Side Login and Client-Side Login
- Brief explanation of each flow

**Dashboard (`/dashboard`):**
- User avatar, name, email from Google
- Badge showing login method (SERVER_SIDE / CLIENT_SIDE)
- Last login timestamp
- Logout button

## Docker Compose

Four services:
1. **traefik** — Reverse proxy on port 80 (dashboard on 8080)
2. **frontend** — React dev server (Vite) on port 5173
3. **backend** — Spring Boot on port 8080
4. **postgres** — PostgreSQL 18.3 on port 5432

Traefik routing:
- `Host(sso.localhost)` priority=1 → frontend
- `Host(sso.localhost) && PathPrefix(/api)` priority=2 → backend

## Environment Variables

```
GOOGLE_CLIENT_ID=<from Google Console>
GOOGLE_CLIENT_SECRET=<from Google Console>
JWT_SECRET=<random 32+ char secret>
```

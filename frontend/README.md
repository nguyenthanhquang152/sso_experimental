# Frontend

React 19 + TypeScript + Vite frontend for the SSO demo.

## Responsibilities

- Fetch runtime provider configuration from `GET /api/auth/providers`
- Render Google and Microsoft server-side/client-side login cards based on that config
- Store JWTs in `localStorage`
- Redirect authenticated users to the dashboard and fetch `/api/user/me`

## Development

```bash
npm install
npm run dev
```

The frontend is normally served through Traefik at `http://localhost:8000`, but Vite can still be used directly for isolated UI work.

## Build and lint

```bash
npm run lint
npm run build
```

## End-to-end tests

```bash
PLAYWRIGHT_API_BASE_URL=http://localhost:8000 npm run test:e2e
```

## Runtime configuration notes

- Google browser login is primarily driven by the backend provider contract.
- `VITE_GOOGLE_CLIENT_ID` remains only as a fallback for local development when backend provider metadata is unavailable.
- Microsoft browser login requires backend-published `clientId`, `authority`, and `scopes`.

## Important files

- `src/App.tsx` — route setup and Google provider bootstrap
- `src/pages/HomePage.tsx` — login landing page and auth-code exchange handling
- `src/hooks/useProviderConfig.ts` — loads `/auth/providers`
- `src/hooks/useAuth.ts` — JWT storage and `/user/me` lifecycle
- `src/components/ClientSideLogin.tsx` — Google browser flow
- `src/components/MicrosoftClientSideLogin.tsx` — Microsoft browser flow

/** Matches backend AuthProvider enum values. */
export type AuthProvider = 'GOOGLE' | 'MICROSOFT';

/** Matches backend AuthFlow enum values. */
export type AuthFlow = 'SERVER_SIDE' | 'CLIENT_SIDE';

const fallbackGoogleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim();

/**
 * Runtime provider configuration contract consumed from `GET /api/auth/providers`.
 *
 * These types mirror the backend `ProviderConfigResponse` record
 * (`backend/src/main/java/com/demo/sso/controller/dto/ProviderConfigResponse.java`).
 * Any field changes must stay in sync across both sides.
 */

/** Google provider availability and public client ID for the browser. */
export interface GoogleProviderConfig {
  serverSideEnabled: boolean;
  clientSideEnabled: boolean;
  clientId?: string;
}

/**
 * Microsoft provider availability and public MSAL configuration.
 *
 * When `clientSideEnabled` is false, optional fields (`clientId`, `authority`,
 * `redirectUri`) are null/undefined and `scopes` is empty.
 */
export interface MicrosoftProviderConfig {
  serverSideEnabled: boolean;
  clientSideEnabled: boolean;
  clientId?: string;
  authority?: string;
  scopes: string[];
  redirectUri?: string;
}

/** Top-level runtime provider config returned by `GET /api/auth/providers`. */
export interface ProviderConfig {
  google: GoogleProviderConfig;
  microsoft: MicrosoftProviderConfig;
}

export const DEFAULT_PROVIDER_CONFIG: ProviderConfig = {
  google: {
    serverSideEnabled: true,
    clientSideEnabled: Boolean(fallbackGoogleClientId),
    clientId: fallbackGoogleClientId || undefined,
  },
  microsoft: {
    serverSideEnabled: false,
    clientSideEnabled: false,
    scopes: [],
  },
};

/** Merge a partial backend response with safe defaults, validating runtime fields. */
export function normalizeProviderConfig(config?: Partial<ProviderConfig> | null): ProviderConfig {
  const google = config?.google;
  const microsoft = config?.microsoft;

  return {
    google: {
      ...DEFAULT_PROVIDER_CONFIG.google,
      ...google,
    },
    microsoft: {
      ...DEFAULT_PROVIDER_CONFIG.microsoft,
      ...microsoft,
      scopes: Array.isArray(microsoft?.scopes) ? microsoft.scopes : [],
      // Clear redirectUri when client-side is disabled so MSAL is never
      // initialised with a stale value from a previous response.
      redirectUri: microsoft?.clientSideEnabled ? (microsoft.redirectUri ?? undefined) : undefined,
    },
  };
}

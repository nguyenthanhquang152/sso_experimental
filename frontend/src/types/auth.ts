const fallbackGoogleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim();

export interface GoogleProviderConfig {
  serverSideEnabled: boolean;
  clientSideEnabled: boolean;
  clientId?: string;
}

export interface MicrosoftProviderConfig {
  serverSideEnabled: boolean;
  clientSideEnabled: boolean;
  clientId?: string;
  authority?: string;
  scopes: string[];
  redirectUri?: string;
}

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
    },
  };
}

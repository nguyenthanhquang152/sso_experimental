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
}

export interface UserProfile {
  id: number;
  email: string;
  name: string;
  pictureUrl: string;
  loginMethod: string;
  provider: string;
  providerUserId: string;
  lastLoginFlow: string;
  createdAt: string;
  lastLoginAt: string;
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

export function mergeProviderConfig(config?: Partial<ProviderConfig> | null): ProviderConfig {
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

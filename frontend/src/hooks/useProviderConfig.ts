import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import {
  DEFAULT_PROVIDER_CONFIG,
  normalizeProviderConfig,
  type ProviderConfig,
} from '../types/auth';

export function useProviderConfig() {
  const [providerConfig, setProviderConfig] = useState<ProviderConfig>(DEFAULT_PROVIDER_CONFIG);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    let cancelled = false;

    apiFetch<ProviderConfig>('/auth/providers')
      .then((response) => {
        if (!cancelled) {
          setProviderConfig(normalizeProviderConfig(response));
          setError(null);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          const wrapped = err instanceof Error ? err : new Error('Failed to load provider config');
          console.warn('Failed to load provider config:', wrapped.message);
          setError(wrapped);
          setProviderConfig(DEFAULT_PROVIDER_CONFIG);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return { providerConfig, loading, error };
}

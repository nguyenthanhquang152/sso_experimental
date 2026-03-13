import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import {
  DEFAULT_PROVIDER_CONFIG,
  mergeProviderConfig,
  type ProviderConfig,
} from '../types/auth';

export function useProviderConfig() {
  const [providerConfig, setProviderConfig] = useState<ProviderConfig>(DEFAULT_PROVIDER_CONFIG);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    apiFetch<ProviderConfig>('/auth/providers')
      .then((response) => {
        if (!cancelled) {
          setProviderConfig(mergeProviderConfig(response));
        }
      })
      .catch(() => {
        if (!cancelled) {
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

  return { providerConfig, loading };
}

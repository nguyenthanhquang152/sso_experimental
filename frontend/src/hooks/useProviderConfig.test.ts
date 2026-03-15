import { renderHook, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useProviderConfig } from './useProviderConfig';
import { DEFAULT_PROVIDER_CONFIG } from '../types/auth';
import type { ProviderConfig } from '../types/auth';

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
}));

describe('useProviderConfig', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns provider config on success', async () => {
    const { apiFetch } = await import('../api/client');
    const mockApiFetch = vi.mocked(apiFetch);

    const mockConfig: ProviderConfig = {
      google: {
        serverSideEnabled: true,
        clientSideEnabled: true,
        clientId: 'google-client-id',
      },
      microsoft: {
        serverSideEnabled: false,
        clientSideEnabled: true,
        clientId: 'ms-client-id',
        authority: 'https://login.microsoftonline.com/tenant',
        scopes: ['openid'],
        redirectUri: 'http://localhost:3000',
      },
    };

    mockApiFetch.mockResolvedValueOnce(mockConfig);

    const { result } = renderHook(() => useProviderConfig());

    // Initially loading
    expect(result.current.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBeNull();
    expect(result.current.providerConfig.google.clientSideEnabled).toBe(true);
    expect(result.current.providerConfig.microsoft.clientSideEnabled).toBe(true);
  });

  it('handles fetch error', async () => {
    const { apiFetch } = await import('../api/client');
    const mockApiFetch = vi.mocked(apiFetch);

    mockApiFetch.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useProviderConfig());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Network error');
    expect(result.current.providerConfig).toEqual(DEFAULT_PROVIDER_CONFIG);
  });

  it('wraps non-Error rejection in Error', async () => {
    const { apiFetch } = await import('../api/client');
    const mockApiFetch = vi.mocked(apiFetch);

    mockApiFetch.mockRejectedValueOnce('string error');

    const { result } = renderHook(() => useProviderConfig());

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('Failed to load provider config');
  });

  it('starts in loading state', async () => {
    const { apiFetch } = await import('../api/client');
    const mockApiFetch = vi.mocked(apiFetch);
    mockApiFetch.mockReturnValueOnce(new Promise(() => {})); // Never resolves

    const { result } = renderHook(() => useProviderConfig());

    expect(result.current.loading).toBe(true);
    expect(result.current.error).toBeNull();
  });
});

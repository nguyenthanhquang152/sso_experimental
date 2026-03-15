import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useAuth } from './useAuth';

describe('useAuth', () => {
  let storage: Record<string, string>;
  let mockFetch: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    storage = {};
    mockFetch = vi.fn();
    vi.stubGlobal('fetch', mockFetch);
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage[key] ?? null,
      setItem: (key: string, value: string) => { storage[key] = value; },
      removeItem: (key: string) => { delete storage[key]; },
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  describe('token management', () => {
    it('starts unauthenticated when no JWT in storage', () => {
      const { result } = renderHook(() => useAuth());

      expect(result.current.token).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
      expect(result.current.loading).toBe(false);
    });

    it('starts authenticated when JWT exists in storage', () => {
      storage['jwt'] = 'existing-token';

      const { result } = renderHook(() => useAuth());

      expect(result.current.token).toBe('existing-token');
      expect(result.current.isAuthenticated).toBe(true);
    });

    it('stores token and becomes authenticated after login', async () => {
      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ id: 1, email: 'test@example.com' }),
      });

      const { result } = renderHook(() => useAuth());

      act(() => {
        result.current.login('new-jwt-token');
      });

      expect(result.current.token).toBe('new-jwt-token');
      expect(result.current.isAuthenticated).toBe(true);
      expect(storage['jwt']).toBe('new-jwt-token');
    });
  });

  describe('logout behavior', () => {
    it('clears token and user on logout', async () => {
      storage['jwt'] = 'some-token';

      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ id: 1, email: 'user@example.com' }),
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.user).not.toBeNull();
      });

      act(() => {
        result.current.logout();
      });

      expect(result.current.token).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.user).toBeNull();
      expect(storage['jwt']).toBeUndefined();
    });

    it('triggers logout automatically on 401 from /user/me', async () => {
      storage['jwt'] = 'expired-token';

      mockFetch.mockResolvedValue({
        ok: false,
        status: 401,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: () => Promise.resolve({ error: 'Unauthorized' }),
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.isAuthenticated).toBe(false);
      });

      expect(result.current.token).toBeNull();
      expect(result.current.user).toBeNull();
    });
  });

  describe('user info fetching', () => {
    it('fetches user profile when token exists', async () => {
      storage['jwt'] = 'valid-token';

      const userProfile = {
        id: 42,
        email: 'alice@example.com',
        name: 'Alice',
        pictureUrl: 'https://example.com/photo.jpg',
        provider: 'GOOGLE',
        providerUserId: 'google-123',
        lastLoginFlow: 'CLIENT_SIDE',
        createdAt: '2024-01-15T10:30:00Z',
        lastLoginAt: '2024-06-01T08:00:00Z',
      };

      mockFetch.mockResolvedValue({
        ok: true,
        status: 200,
        json: () => Promise.resolve(userProfile),
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
        expect(result.current.user).not.toBeNull();
      });

      expect(result.current.user).toEqual(userProfile);
    });

    it('exposes error state on non-401 fetch error', async () => {
      storage['jwt'] = 'valid-token';

      mockFetch.mockResolvedValue({
        ok: false,
        status: 500,
        headers: new Headers({ 'content-type': 'text/plain' }),
        text: () => Promise.resolve('Internal Server Error'),
      });

      const { result } = renderHook(() => useAuth());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.user).toBeNull();
      expect(result.current.error?.message).toBe('Internal Server Error');
      // Token is NOT cleared on non-401 errors
      expect(result.current.isAuthenticated).toBe(true);
    });
  });
});

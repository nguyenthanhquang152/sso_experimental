import { useState, useEffect, useCallback } from 'react';
import { apiFetch, ApiError } from '../api/client';
import type { AuthProvider, AuthFlow } from '../types/auth';

interface UserProfile {
  id: number;
  email: string;
  name: string;
  pictureUrl: string;
  provider: AuthProvider;
  providerUserId: string;
  lastLoginFlow: AuthFlow | null;
  createdAt: string;
  lastLoginAt: string;
}

export function useAuth() {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('jwt')
  );
  const [user, setUser] = useState<UserProfile | null | undefined>(() =>
    localStorage.getItem('jwt') ? undefined : null
  );
  const [error, setError] = useState<Error | null>(null);

  const login = useCallback((jwt: string) => {
    localStorage.setItem('jwt', jwt);
    setToken(jwt);
    setUser(undefined);
    setError(null);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('jwt');
    setToken(null);
    setUser(null);
    setError(null);
  }, []);

  useEffect(() => {
    if (!token) {
      return;
    }

    apiFetch<UserProfile>('/user/me')
      .then((profile) => {
        setUser(profile);
        setError(null);
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          console.warn('Session expired, logging out');
          logout();
        } else {
          const message = err instanceof Error ? err.message : String(err);
          console.warn('Failed to fetch user profile:', message);
          setUser(null);
          setError(new Error(message));
        }
      });
  }, [token, logout]);

  return {
    token,
    user: user ?? null,
    loading: Boolean(token) && user === undefined,
    error,
    login,
    logout,
    isAuthenticated: !!token,
  };
}

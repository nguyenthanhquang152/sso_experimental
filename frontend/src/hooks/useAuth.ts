import { useState, useEffect, useCallback } from 'react';
import { apiFetch } from '../api/client';
import type { UserProfile } from '../types/auth';

export function useAuth() {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('jwt')
  );
  const [user, setUser] = useState<UserProfile | null | undefined>(() =>
    localStorage.getItem('jwt') ? undefined : null
  );

  const login = useCallback((jwt: string) => {
    localStorage.setItem('jwt', jwt);
    setToken(jwt);
    setUser(undefined);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('jwt');
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    if (!token) {
      return;
    }

    apiFetch<UserProfile>('/user/me')
      .then(setUser)
      .catch(() => {
        logout();
      });
  }, [token, logout]);

  return {
    token,
    user: user ?? null,
    loading: Boolean(token) && user === undefined,
    login,
    logout,
    isAuthenticated: !!token,
  };
}

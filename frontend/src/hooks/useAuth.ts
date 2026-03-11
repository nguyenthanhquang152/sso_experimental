import { useState, useEffect, useCallback } from 'react';
import { apiFetch } from '../api/client';

interface UserProfile {
  id: number;
  email: string;
  name: string;
  pictureUrl: string;
  loginMethod: string;
  createdAt: string;
  lastLoginAt: string;
}

export function useAuth() {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('jwt')
  );
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(false);

  const login = useCallback((jwt: string) => {
    localStorage.setItem('jwt', jwt);
    setToken(jwt);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('jwt');
    setToken(null);
    setUser(null);
  }, []);

  useEffect(() => {
    if (!token) {
      setUser(null);
      return;
    }

    setLoading(true);
    apiFetch<UserProfile>('/user/me')
      .then(setUser)
      .catch(() => {
        logout();
      })
      .finally(() => setLoading(false));
  }, [token, logout]);

  return { token, user, loading, login, logout, isAuthenticated: !!token };
}

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { App } from './App';

vi.mock('./hooks/useProviderConfig', () => ({
  useProviderConfig: () => ({
    providerConfig: {
      google: { clientId: '', enabled: false },
      microsoft: { clientId: '', tenantId: '', enabled: false },
    },
    loading: false,
    error: null,
  }),
}));

vi.mock('./hooks/useAuth', () => ({
  useAuth: () => ({
    token: null,
    user: null,
    loading: false,
    error: null,
    login: vi.fn(),
    logout: vi.fn(),
    isAuthenticated: false,
  }),
}));

describe('App', () => {
  it('renders without crashing', () => {
    render(<App />);
    // HomePage is the default route and renders heading content
    expect(document.body.innerHTML.length).toBeGreaterThan(0);
  });

  it('renders the home page at default route', () => {
    render(<App />);
    // HomePage should be visible at "/"
    expect(screen.getByRole('heading')).toBeDefined();
  });
});

import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { HomePage } from './HomePage';
import type { ProviderConfig } from '../types/auth';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
  useSearchParams: () => [new URLSearchParams()],
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: vi.fn(() => ({
    login: vi.fn(),
    isAuthenticated: false,
    token: null,
    user: null,
    loading: false,
    logout: vi.fn(),
  })),
}));

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
  ApiError: class ApiError extends Error {
    status: number;
    constructor(message: string, status: number) {
      super(message);
      this.status = status;
    }
  },
  getErrorMessage: (_err: unknown, fallback: string) => fallback,
}));

vi.mock('../components/ServerSideLogin', () => ({
  ServerSideLogin: (props: { providerName: string }) => (
    <div data-testid={`server-login-${props.providerName.toLowerCase()}`}>
      {props.providerName} Server-Side
    </div>
  ),
}));

vi.mock('../components/ClientSideLogin', () => ({
  ClientSideLogin: () => <div data-testid="client-login-google">Google Client-Side</div>,
}));

vi.mock('../components/MicrosoftClientSideLogin', () => ({
  MicrosoftClientSideLogin: () => (
    <div data-testid="client-login-microsoft">Microsoft Client-Side</div>
  ),
}));

const baseConfig: ProviderConfig = {
  google: { serverSideEnabled: false, clientSideEnabled: false },
  microsoft: { serverSideEnabled: false, clientSideEnabled: false, scopes: [] },
};

describe('HomePage', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it('renders login cards based on provider config', () => {
    render(<HomePage providerConfig={baseConfig} />);

    expect(screen.getByText('SSO Demo')).toBeInTheDocument();
    expect(screen.queryByTestId('server-login-google')).not.toBeInTheDocument();
    expect(screen.queryByTestId('client-login-google')).not.toBeInTheDocument();
  });

  it('shows Google card when google server-side is enabled', () => {
    const config: ProviderConfig = {
      ...baseConfig,
      google: { serverSideEnabled: true, clientSideEnabled: false },
    };
    render(<HomePage providerConfig={config} />);

    expect(screen.getByTestId('server-login-google')).toBeInTheDocument();
  });

  it('shows Microsoft client-side card when microsoft client-side is enabled', () => {
    const config: ProviderConfig = {
      ...baseConfig,
      microsoft: { serverSideEnabled: false, clientSideEnabled: true, scopes: ['openid'] },
    };
    render(<HomePage providerConfig={config} />);

    expect(screen.getByTestId('client-login-microsoft')).toBeInTheDocument();
  });

  it('redirects to dashboard when user is already authenticated', async () => {
    const { useAuth } = await import('../hooks/useAuth');
    vi.mocked(useAuth).mockReturnValue({
      login: vi.fn(),
      isAuthenticated: true,
      token: 'existing-jwt',
      user: null,
      loading: false,
      error: null,
      logout: vi.fn(),
    });

    render(<HomePage providerConfig={baseConfig} />);

    expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true });
  });
});

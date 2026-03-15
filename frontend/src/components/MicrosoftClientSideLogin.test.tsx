import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MicrosoftClientSideLogin } from './MicrosoftClientSideLogin';
import type { MicrosoftProviderConfig } from '../types/auth';

const mockInitialize = vi.fn().mockResolvedValue(undefined);
const mockLoginPopup = vi.fn();

vi.mock('@azure/msal-browser', () => {
  const MockPCA = vi.fn().mockImplementation(function (this: Record<string, unknown>) {
    this.initialize = mockInitialize;
    this.loginPopup = mockLoginPopup;
  });
  return { PublicClientApplication: MockPCA };
});

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
  getErrorMessage: (_err: unknown, fallback: string) => fallback,
}));

const validConfig: MicrosoftProviderConfig = {
  serverSideEnabled: false,
  clientSideEnabled: true,
  clientId: 'test-client-id',
  authority: 'https://login.microsoftonline.com/test-tenant',
  scopes: ['openid', 'profile', 'email'],
  redirectUri: 'http://localhost:3000',
};

describe('MicrosoftClientSideLogin', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders login button', () => {
    render(<MicrosoftClientSideLogin config={validConfig} onSuccess={vi.fn()} />);

    expect(screen.getByRole('button', { name: /continue with microsoft/i })).toBeInTheDocument();
  });

  it('disables button when config is incomplete', () => {
    const incompleteConfig: MicrosoftProviderConfig = {
      ...validConfig,
      clientId: undefined,
      authority: undefined,
    };

    render(<MicrosoftClientSideLogin config={incompleteConfig} onSuccess={vi.fn()} />);

    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('disables button when scopes are empty', () => {
    const noScopesConfig: MicrosoftProviderConfig = {
      ...validConfig,
      scopes: [],
    };

    render(<MicrosoftClientSideLogin config={noScopesConfig} onSuccess={vi.fn()} />);

    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('handles login click and calls onSuccess', async () => {
    const { apiFetch } = await import('../api/client');
    const mockApiFetch = vi.mocked(apiFetch);

    mockApiFetch
      .mockResolvedValueOnce({ challengeId: 'ch-1', nonce: 'nonce-1' })
      .mockResolvedValueOnce({ token: 'jwt-token-123' });

    mockLoginPopup.mockResolvedValueOnce({ idToken: 'ms-id-token' });

    const onSuccess = vi.fn();
    render(<MicrosoftClientSideLogin config={validConfig} onSuccess={onSuccess} />);

    await userEvent.click(screen.getByRole('button', { name: /continue with microsoft/i }));

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalledWith('jwt-token-123');
    });

    expect(mockApiFetch).toHaveBeenCalledWith('/auth/microsoft/challenge', {
      method: 'POST',
      credentials: 'include',
    });

    expect(mockLoginPopup).toHaveBeenCalledWith({
      scopes: ['openid', 'profile', 'email'],
      nonce: 'nonce-1',
      prompt: 'select_account',
    });

    expect(mockApiFetch).toHaveBeenCalledWith('/auth/microsoft/verify', {
      method: 'POST',
      credentials: 'include',
      body: JSON.stringify({
        credential: 'ms-id-token',
        challengeId: 'ch-1',
      }),
    });
  });

  it('shows error state when login fails', async () => {
    const { apiFetch } = await import('../api/client');
    const mockApiFetch = vi.mocked(apiFetch);
    mockApiFetch.mockRejectedValueOnce(new Error('Network error'));

    render(<MicrosoftClientSideLogin config={validConfig} onSuccess={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: /continue with microsoft/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Microsoft sign-in failed. Please try again.',
      );
    });
  });

  it('shows loading state during auth', async () => {
    const { apiFetch } = await import('../api/client');
    const mockApiFetch = vi.mocked(apiFetch);

    // Create a promise that we control to keep the login in progress
    let resolveChallenge!: (value: unknown) => void;
    const challengePromise = new Promise((resolve) => {
      resolveChallenge = resolve;
    });
    mockApiFetch.mockReturnValueOnce(challengePromise as ReturnType<typeof apiFetch>);

    render(<MicrosoftClientSideLogin config={validConfig} onSuccess={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: /continue with microsoft/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /signing in with microsoft/i })).toBeDisabled();
    });

    // Cleanup: resolve the pending promise
    resolveChallenge({ challengeId: 'ch', nonce: 'n' });
    mockLoginPopup.mockResolvedValueOnce({ idToken: 'tok' });
    mockApiFetch.mockResolvedValueOnce({ token: 'jwt' });
  });
});

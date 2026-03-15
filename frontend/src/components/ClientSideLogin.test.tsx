import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ClientSideLogin } from './ClientSideLogin';
import type { CredentialResponse } from '@react-oauth/google';

// Capture the handlers passed to GoogleLogin
let capturedOnSuccess: ((resp: CredentialResponse) => void) | undefined;
let capturedOnError: (() => void) | undefined;

vi.mock('@react-oauth/google', () => ({
  GoogleLogin: (props: { onSuccess: (resp: CredentialResponse) => void; onError: () => void }) => {
    capturedOnSuccess = props.onSuccess;
    capturedOnError = props.onError;
    return <button data-testid="google-login-btn">Sign in with Google</button>;
  },
}));

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
  getErrorMessage: (_err: unknown, fallback: string) => fallback,
}));

describe('ClientSideLogin', () => {
  beforeEach(() => {
    capturedOnSuccess = undefined;
    capturedOnError = undefined;
  });

  it('renders Google login button when clientReady is true', () => {
    render(<ClientSideLogin onSuccess={vi.fn()} clientReady={true} />);

    expect(screen.getByTestId('google-login-btn')).toBeInTheDocument();
  });

  it('renders fallback button when clientReady is false', () => {
    render(<ClientSideLogin onSuccess={vi.fn()} clientReady={false} />);

    expect(screen.getByRole('button', { name: /temporarily unavailable/i })).toBeDisabled();
    expect(screen.queryByTestId('google-login-btn')).not.toBeInTheDocument();
  });

  it('calls onSuccess with token on successful credential response', async () => {
    const { apiFetch } = await import('../api/client');
    const mockApiFetch = vi.mocked(apiFetch);
    mockApiFetch.mockResolvedValueOnce({ token: 'jwt-token-123' });

    const onSuccess = vi.fn();
    render(<ClientSideLogin onSuccess={onSuccess} clientReady={true} />);

    // Simulate Google returning a credential
    await capturedOnSuccess!({ credential: 'google-id-token', select_by: 'btn' });

    expect(mockApiFetch).toHaveBeenCalledWith('/auth/google/verify', {
      method: 'POST',
      body: JSON.stringify({ credential: 'google-id-token' }),
    });
    expect(onSuccess).toHaveBeenCalledWith('jwt-token-123');
  });

  it('shows error when Google sign-in fails', () => {
    render(<ClientSideLogin onSuccess={vi.fn()} clientReady={true} />);

    act(() => {
      capturedOnError!();
    });

    expect(screen.getByRole('alert')).toHaveTextContent('Google sign-in failed');
  });
});

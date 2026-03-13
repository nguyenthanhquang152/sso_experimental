import { GoogleLogin } from '@react-oauth/google';
import type { CredentialResponse } from '@react-oauth/google';
import { apiFetch } from '../api/client';
import { LoginCard } from './LoginCard';

interface ClientSideLoginProps {
  onSuccess: (token: string) => void;
  clientReady: boolean;
}

const fallbackButtonStyle = {
  width: '100%',
  padding: '12px 16px',
  backgroundColor: '#e5e7eb',
  color: '#4b5563',
  border: 'none',
  borderRadius: '8px',
  fontWeight: 700,
  cursor: 'not-allowed',
} as const;

export function ClientSideLogin({ onSuccess, clientReady }: ClientSideLoginProps) {
  const handleSuccess = async (credentialResponse: CredentialResponse) => {
    if (!credentialResponse.credential) return;

    try {
      const data = await apiFetch<{ token: string }>('/auth/google/verify', {
        method: 'POST',
        body: JSON.stringify({ credential: credentialResponse.credential }),
      });
      onSuccess(data.token);
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  return (
    <LoginCard
      title="Google · Client-Side Flow"
      description="Google Sign-In happens directly in the browser via a popup. The ID token is sent to the backend for verification."
      accentColor="#4285f4"
    >
      {clientReady ? (
        <GoogleLogin
          onSuccess={handleSuccess}
          onError={() => console.error('Google Login Failed')}
        />
      ) : (
        <button type="button" disabled style={fallbackButtonStyle}>
          Google client login is temporarily unavailable
        </button>
      )}
    </LoginCard>
  );
}

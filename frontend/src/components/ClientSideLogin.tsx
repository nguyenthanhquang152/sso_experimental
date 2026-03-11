import { GoogleLogin, CredentialResponse } from '@react-oauth/google';
import { apiFetch } from '../api/client';

interface ClientSideLoginProps {
  onSuccess: (token: string) => void;
}

export function ClientSideLogin({ onSuccess }: ClientSideLoginProps) {
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
    <div style={{
      border: '1px solid #e0e0e0',
      borderRadius: '8px',
      padding: '24px',
      maxWidth: '400px',
    }}>
      <h2>Client-Side Flow</h2>
      <p style={{ color: '#666', fontSize: '14px' }}>
        Google Sign-In happens directly in the browser via a popup.
        The ID token is sent to the backend for verification.
      </p>
      <GoogleLogin
        onSuccess={handleSuccess}
        onError={() => console.error('Google Login Failed')}
      />
    </div>
  );
}

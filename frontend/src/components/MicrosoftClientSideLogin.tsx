import { useMemo, useState } from 'react';
import { PublicClientApplication } from '@azure/msal-browser';
import { apiFetch, getErrorMessage } from '../api/client';
import { LoginCard } from './LoginCard';
import type { MicrosoftProviderConfig } from '../types/auth';
import styles from './MicrosoftClientSideLogin.module.css';

interface MicrosoftClientSideLoginProps {
  config: MicrosoftProviderConfig;
  onSuccess: (token: string) => void;
}

interface MicrosoftChallengeResponse {
  challengeId: string;
  nonce: string;
}

export function MicrosoftClientSideLogin({ config, onSuccess }: MicrosoftClientSideLoginProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const msalClient = useMemo(() => {
    if (!config.clientId || !config.authority) {
      return null;
    }

    return new PublicClientApplication({
      auth: {
        clientId: config.clientId,
        authority: config.authority,
        redirectUri: config.redirectUri ?? window.location.origin,
      },
      cache: {
        cacheLocation: 'sessionStorage',
      },
    });
  }, [config.authority, config.clientId, config.redirectUri]);

  const ready = Boolean(msalClient && config.scopes.length > 0);

  const handleLogin = async () => {
    if (!msalClient || !ready || submitting) {
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await msalClient.initialize();
      const challenge = await apiFetch<MicrosoftChallengeResponse>('/auth/microsoft/challenge', {
        method: 'POST',
        credentials: 'include',
      });
      if (!challenge) {
        throw new Error('No response from challenge endpoint');
      }

      const response = await msalClient.loginPopup({
        scopes: config.scopes,
        nonce: challenge.nonce,
        prompt: 'select_account',
      });

      if (!response.idToken) {
        throw new Error('Microsoft did not return an ID token');
      }

      const verification = await apiFetch<{ token: string }>('/auth/microsoft/verify', {
        method: 'POST',
        credentials: 'include',
        body: JSON.stringify({
          credential: response.idToken,
          challengeId: challenge.challengeId,
        }),
      });
      if (!verification) {
        throw new Error('No response from verification endpoint');
      }

      onSuccess(verification.token);
    } catch (loginError) {
      setError(getErrorMessage(loginError, 'Microsoft sign-in failed. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <LoginCard
      title="Microsoft · Client-Side Flow"
      description="Microsoft signs in directly in the browser, then the ID token is verified by the backend."
      accentColor="#2f2f2f"
      footer={
        error ? (
          <p className={styles.errorMessage} role="alert">
            {error}
          </p>
        ) : null
      }
    >
      <button
        type="button"
        onClick={handleLogin}
        disabled={!ready || submitting}
        className={`${styles.button}${!ready || submitting ? ` ${styles.disabled}` : ''}`}
      >
        {submitting ? 'Signing in with Microsoft…' : 'Continue with Microsoft'}
      </button>
    </LoginCard>
  );
}

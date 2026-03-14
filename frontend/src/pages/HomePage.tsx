import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ServerSideLogin } from '../components/ServerSideLogin';
import { ClientSideLogin } from '../components/ClientSideLogin';
import { MicrosoftClientSideLogin } from '../components/MicrosoftClientSideLogin';
import { useAuth } from '../hooks/useAuth';
import { ApiError, apiFetch, getErrorMessage } from '../api/client';
import type { ProviderConfig } from '../types/auth';

interface HomePageProps {
  providerConfig: ProviderConfig;
}

export function HomePage({ providerConfig }: HomePageProps) {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [exchangeError, setExchangeError] = useState<string | null>(() => {
    const errorParam = new URLSearchParams(window.location.search).get('error');
    if (errorParam) {
      window.history.replaceState({}, '', '/');
      return `Sign-in failed: ${errorParam.replace(/_/g, ' ')}`;
    }
    return null;
  });

  useEffect(() => {
    const code = searchParams.get('code');
    if (code) {
      // Strip code from URL immediately to prevent leakage
      window.history.replaceState({}, '', '/');

      // Exchange the single-use code for a JWT
      apiFetch<{ token: string }>('/auth/exchange', {
        method: 'POST',
        body: JSON.stringify({ code }),
      })
        .then((data) => {
          login(data.token);
          navigate('/dashboard', { replace: true });
        })
        .catch((error: unknown) => {
          if (error instanceof ApiError && error.status === 400) {
            setExchangeError(error.message);
            return;
          }

          setExchangeError(getErrorMessage(error, 'Sign-in could not be completed. Please try again.'));
        });
    }
  }, [searchParams, login, navigate]);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const handleClientLogin = (token: string) => {
    login(token);
    navigate('/dashboard');
  };

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto', padding: '40px 20px' }}>
      <h1>SSO Demo</h1>
      <p style={{ color: '#666', marginBottom: '32px' }}>
        Choose a provider and flow to sign in.
      </p>
      {exchangeError ? (
        <p
          style={{ color: '#b91c1c', marginBottom: '20px', fontWeight: 500 }}
          role="alert"
        >
          {exchangeError}
        </p>
      ) : null}
      <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
        {providerConfig.google.serverSideEnabled ? (
          <ServerSideLogin
            providerName="Google"
            flowLabel="Server-Side Flow"
            description="The browser redirects to Google via the Spring Boot backend. Spring Security handles the full authorization code exchange."
            buttonLabel="Continue with Google (Server-Side)"
            href="/api/oauth2/authorization/google"
            accentColor="#4285f4"
          />
        ) : null}

        {providerConfig.google.clientSideEnabled ? (
          <ClientSideLogin
            onSuccess={handleClientLogin}
            clientReady={Boolean(providerConfig.google.clientId)}
          />
        ) : null}

        {providerConfig.microsoft.serverSideEnabled ? (
          <ServerSideLogin
            providerName="Microsoft"
            flowLabel="Server-Side Flow"
            description="The browser redirects to Microsoft Entra ID via the backend, which completes the authorization code flow."
            buttonLabel="Continue with Microsoft (Server-Side)"
            href="/api/oauth2/authorization/microsoft"
            accentColor="#2f2f2f"
          />
        ) : null}

        {providerConfig.microsoft.clientSideEnabled ? (
          <MicrosoftClientSideLogin
            config={providerConfig.microsoft}
            onSuccess={handleClientLogin}
          />
        ) : null}
      </div>
    </div>
  );
}

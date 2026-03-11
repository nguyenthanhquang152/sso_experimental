import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ServerSideLogin } from '../components/ServerSideLogin';
import { ClientSideLogin } from '../components/ClientSideLogin';
import { useAuth } from '../hooks/useAuth';
import { apiFetch } from '../api/client';

export function HomePage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

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
        .catch(() => {
          // Code was invalid or expired — stay on homepage
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
      <h1>Google SSO Demo</h1>
      <p style={{ color: '#666', marginBottom: '32px' }}>
        This demo shows two Google OAuth2 flows side-by-side.
        Choose either method to sign in.
      </p>
      <div style={{ display: 'flex', gap: '24px', flexWrap: 'wrap' }}>
        <ServerSideLogin />
        <ClientSideLogin onSuccess={handleClientLogin} />
      </div>
    </div>
  );
}

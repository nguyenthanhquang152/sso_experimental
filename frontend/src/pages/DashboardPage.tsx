import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useEffect } from 'react';

function getAvatarInitials(name: string, email: string) {
  const source = name.trim() || email.trim();
  const words = source
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2);

  if (words.length === 0) {
    return '?';
  }

  if (words.length === 1) {
    return words[0].slice(0, 2).toUpperCase();
  }

  return words.map((word) => word[0]?.toUpperCase() ?? '').join('');
}

export function DashboardPage() {
  const { user, loading, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  if (loading) {
    return <div style={{ padding: '40px', textAlign: 'center' }}>Loading...</div>;
  }

  if (!user) {
    return null;
  }

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const avatarInitials = getAvatarInitials(user.name, user.email);

  const avatarStyle = {
    width: '64px',
    height: '64px',
    borderRadius: '50%',
    flexShrink: 0,
  } as const;

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto', padding: '40px 20px' }}>
      <h1>Dashboard</h1>
      <div style={{
        border: '1px solid #e0e0e0',
        borderRadius: '8px',
        padding: '24px',
        marginTop: '20px',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', marginBottom: '20px' }}>
          {user.pictureUrl ? (
            <img
              src={user.pictureUrl}
              alt={user.name}
              referrerPolicy="no-referrer"
              crossOrigin="anonymous"
              style={avatarStyle}
            />
          ) : (
            <div
              role="img"
              aria-label={`Avatar fallback for ${user.name}`}
              style={{
                ...avatarStyle,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backgroundColor: '#e3f2fd',
                color: '#1565c0',
                fontWeight: 700,
                fontSize: '24px',
              }}
            >
              {avatarInitials}
            </div>
          )}
          <div>
            <h2 style={{ margin: 0 }}>{user.name}</h2>
            <p style={{ margin: '4px 0', color: '#666' }}>{user.email}</p>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '14px' }}>
          <div>
            <strong>Login Method: </strong>
            <span style={{
              padding: '2px 8px',
              borderRadius: '4px',
              backgroundColor: user.loginMethod === 'SERVER_SIDE' ? '#e3f2fd' : '#f3e5f5',
              color: user.loginMethod === 'SERVER_SIDE' ? '#1565c0' : '#7b1fa2',
              fontWeight: 'bold',
              fontSize: '12px',
            }}>
              {user.loginMethod}
            </span>
          </div>
          <div>
            <strong>Last Login: </strong>
            {new Date(user.lastLoginAt).toLocaleString()}
          </div>
          <div>
            <strong>Account Created: </strong>
            {new Date(user.createdAt).toLocaleString()}
          </div>
        </div>

        <button
          onClick={handleLogout}
          style={{
            marginTop: '24px',
            padding: '10px 24px',
            backgroundColor: '#f44336',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontWeight: 'bold',
          }}
        >
          Logout
        </button>
      </div>
    </div>
  );
}

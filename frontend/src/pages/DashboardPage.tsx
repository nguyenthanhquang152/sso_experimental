import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useEffect } from 'react';
import './DashboardPage.css';

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
    return <div className="dashboard-loading">Loading...</div>;
  }

  if (!user) {
    return null;
  }

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const avatarInitials = getAvatarInitials(user.name, user.email);
  const badgeClass = user.loginMethod === 'SERVER_SIDE'
    ? 'login-method-badge login-method-badge--server-side'
    : 'login-method-badge login-method-badge--client-side';

  return (
    <div className="dashboard-container">
      <h1>Dashboard</h1>
      <div className="dashboard-card">
        <div className="dashboard-header">
          {user.pictureUrl ? (
            <img
              src={user.pictureUrl}
              alt={user.name}
              referrerPolicy="no-referrer"
              crossOrigin="anonymous"
              className="dashboard-avatar"
            />
          ) : (
            <div
              role="img"
              aria-label={`Avatar fallback for ${user.name}`}
              className="dashboard-avatar-fallback"
            >
              {avatarInitials}
            </div>
          )}
          <div>
            <h2 className="dashboard-user-name">{user.name}</h2>
            <p className="dashboard-user-email">{user.email}</p>
          </div>
        </div>

        <div className="dashboard-details">
          <div>
            <strong>Login Method: </strong>
            <span className={badgeClass}>
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

        <button onClick={handleLogout} className="dashboard-logout-btn">
          Logout
        </button>
      </div>
    </div>
  );
}

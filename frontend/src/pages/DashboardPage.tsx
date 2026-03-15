import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useEffect } from 'react';
import styles from './DashboardPage.module.css';

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
  const { user, loading, logout, isAuthenticated, error } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  if (loading) {
    return <div className={styles.loadingContainer}>Loading...</div>;
  }

  if (error) {
    return <div className={styles.error}>{error}</div>;
  }

  if (!user) {
    return null;
  }

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const avatarInitials = getAvatarInitials(user.name, user.email);

  return (
    <div className={styles.container}>
      <h1>Dashboard</h1>
      <div className={styles.card}>
        <div className={styles.profileRow}>
          {user.pictureUrl ? (
            <img
              src={user.pictureUrl}
              alt={user.name}
              referrerPolicy="no-referrer"
              crossOrigin="anonymous"
              className={styles.avatar}
            />
          ) : (
            <div
              role="img"
              aria-label={`Avatar fallback for ${user.name}`}
              className={styles.avatarFallback}
            >
              {avatarInitials}
            </div>
          )}
          <div>
            <h2 className={styles.userName}>{user.name}</h2>
            <p className={styles.userEmail}>{user.email}</p>
          </div>
        </div>

        <div className={styles.infoGrid}>
          <div>
            <strong>Login Method: </strong>
            <span className={user.lastLoginFlow === 'SERVER_SIDE' ? styles.badgeServerSide : styles.badgeClientSide}>
              {user.lastLoginFlow}
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
          className={styles.logoutButton}
        >
          Logout
        </button>
      </div>
    </div>
  );
}

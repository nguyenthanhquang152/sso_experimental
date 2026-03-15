import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { DashboardPage } from './DashboardPage';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    user: {
      id: 1,
      email: 'test@example.com',
      name: 'Test User',
      pictureUrl: '',
      provider: 'GOOGLE',
      providerUserId: 'g-1',
      lastLoginFlow: 'CLIENT_SIDE',
      createdAt: '2024-01-01T00:00:00Z',
      lastLoginAt: '2024-06-01T00:00:00Z',
    },
    loading: false,
    error: null,
    logout: vi.fn(),
    isAuthenticated: true,
  }),
}));

describe('DashboardPage', () => {
  it('renders user information', () => {
    render(<DashboardPage />);
    expect(screen.getByText('test@example.com')).toBeDefined();
    expect(screen.getByText('Test User')).toBeDefined();
  });

  it('renders the Dashboard heading', () => {
    render(<DashboardPage />);
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeDefined();
  });

  it('renders a logout button', () => {
    render(<DashboardPage />);
    expect(screen.getByRole('button', { name: /logout/i })).toBeDefined();
  });

  it('shows avatar fallback initials when no picture', () => {
    render(<DashboardPage />);
    expect(screen.getByRole('img', { name: /avatar fallback/i })).toBeDefined();
    expect(screen.getByText('TU')).toBeDefined();
  });
});

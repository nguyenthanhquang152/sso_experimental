import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { LoginCard } from './LoginCard';

describe('LoginCard', () => {
  it('renders provider name and description', () => {
    render(
      <LoginCard title="Google · Client-Side Flow" description="Sign in with Google" accentColor="#4285f4">
        <button>Login</button>
      </LoginCard>
    );

    expect(screen.getByText('Google · Client-Side Flow')).toBeInTheDocument();
    expect(screen.getByText('Sign in with Google')).toBeInTheDocument();
  });

  it('renders children (login button slot)', () => {
    render(
      <LoginCard title="Test" description="desc" accentColor="#000">
        <button>Continue with Google</button>
      </LoginCard>
    );

    expect(screen.getByRole('button', { name: 'Continue with Google' })).toBeInTheDocument();
  });

  it('renders footer when provided', () => {
    render(
      <LoginCard
        title="Test"
        description="desc"
        accentColor="#000"
        footer={<p role="alert">Something went wrong</p>}
      >
        <button>Login</button>
      </LoginCard>
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong');
  });

  it('does not render footer when not provided', () => {
    render(
      <LoginCard title="Test" description="desc" accentColor="#000">
        <button>Login</button>
      </LoginCard>
    );

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

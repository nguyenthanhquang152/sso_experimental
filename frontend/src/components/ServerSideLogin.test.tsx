import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ServerSideLogin } from './ServerSideLogin';

describe('ServerSideLogin', () => {
  const defaultProps = {
    providerName: 'Google',
    flowLabel: 'Server-Side',
    description: 'Sign in via server redirect',
    buttonLabel: 'Sign in with Google',
    href: '/api/auth/google',
    accentColor: '#4285F4',
  };

  it('renders the login button with correct label', () => {
    render(<ServerSideLogin {...defaultProps} />);
    expect(screen.getByText('Sign in with Google')).toBeDefined();
  });

  it('renders the provider title', () => {
    render(<ServerSideLogin {...defaultProps} />);
    expect(screen.getByText('Google · Server-Side')).toBeDefined();
  });

  it('renders the description', () => {
    render(<ServerSideLogin {...defaultProps} />);
    expect(screen.getByText('Sign in via server redirect')).toBeDefined();
  });

  it('renders the button as a link with correct href', () => {
    render(<ServerSideLogin {...defaultProps} />);
    const link = screen.getByText('Sign in with Google');
    expect(link.tagName).toBe('A');
    expect(link.getAttribute('href')).toBe('/api/auth/google');
  });

  it('applies accent color to the button', () => {
    render(<ServerSideLogin {...defaultProps} />);
    const link = screen.getByText('Sign in with Google');
    expect(link.style.backgroundColor).toBe('rgb(66, 133, 244)');
  });
});

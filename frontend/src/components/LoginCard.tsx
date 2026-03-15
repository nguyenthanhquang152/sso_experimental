import type { ReactNode } from 'react';
import './LoginCard.css';

interface LoginCardProps {
  title: string;
  description: string;
  accentColor: string;
  children: ReactNode;
  footer?: ReactNode;
}

export function LoginCard({ title, description, accentColor, children, footer }: LoginCardProps) {
  return (
    <div className="login-card">
      <div
        className="login-card__accent"
        style={{ backgroundColor: accentColor }}
      />
      <h2>{title}</h2>
      <p className="login-card__description">{description}</p>
      <div className="login-card__body">{children}</div>
      {footer ? <div className="login-card__footer">{footer}</div> : null}
    </div>
  );
}

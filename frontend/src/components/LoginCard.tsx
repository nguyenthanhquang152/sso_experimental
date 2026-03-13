import type { CSSProperties, ReactNode } from 'react';

interface LoginCardProps {
  title: string;
  description: string;
  accentColor: string;
  children: ReactNode;
  footer?: ReactNode;
}

const cardStyle: CSSProperties = {
  border: '1px solid #e0e0e0',
  borderRadius: '12px',
  padding: '24px',
  maxWidth: '400px',
  flex: '1 1 320px',
  boxShadow: '0 8px 24px rgba(15, 23, 42, 0.08)',
  background: '#fff',
};

export function LoginCard({ title, description, accentColor, children, footer }: LoginCardProps) {
  return (
    <div style={cardStyle}>
      <div
        style={{
          width: '48px',
          height: '4px',
          borderRadius: '999px',
          backgroundColor: accentColor,
          marginBottom: '16px',
        }}
      />
      <h2>{title}</h2>
      <p style={{ color: '#666', fontSize: '14px', minHeight: '48px' }}>{description}</p>
      <div style={{ marginTop: '20px' }}>{children}</div>
      {footer ? <div style={{ marginTop: '12px' }}>{footer}</div> : null}
    </div>
  );
}

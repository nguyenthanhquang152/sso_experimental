import type { ReactNode } from 'react';
import styles from './LoginCard.module.css';

interface LoginCardProps {
  title: string;
  description: string;
  accentColor: string;
  children: ReactNode;
  footer?: ReactNode;
}

export function LoginCard({ title, description, accentColor, children, footer }: LoginCardProps) {
  return (
    <div className={styles.card}>
      <div
        className={styles.accentBar}
        style={{ backgroundColor: accentColor }}
      />
      <h2>{title}</h2>
      <p className={styles.description}>{description}</p>
      <div className={styles.body}>{children}</div>
      {footer ? <div className={styles.footer}>{footer}</div> : null}
    </div>
  );
}

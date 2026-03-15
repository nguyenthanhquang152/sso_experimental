import { LoginCard } from './LoginCard';
import styles from './ServerSideLogin.module.css';

interface ServerSideLoginProps {
  providerName: string;
  flowLabel: string;
  description: string;
  buttonLabel: string;
  href: string;
  accentColor: string;
}

export function ServerSideLogin({
  providerName,
  flowLabel,
  description,
  buttonLabel,
  href,
  accentColor,
}: ServerSideLoginProps) {
  return (
    <LoginCard
      title={`${providerName} · ${flowLabel}`}
      description={description}
      accentColor={accentColor}
    >
      <a
        href={href}
        className={styles.button}
        style={{ backgroundColor: accentColor, color: 'white' }}
      >
        {buttonLabel}
      </a>
    </LoginCard>
  );
}

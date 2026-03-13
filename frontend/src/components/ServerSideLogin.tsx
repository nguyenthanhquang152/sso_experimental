import { LoginCard } from './LoginCard';

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
        style={{
          display: 'inline-block',
          padding: '10px 24px',
          backgroundColor: accentColor,
          color: 'white',
          textDecoration: 'none',
          borderRadius: '8px',
          fontWeight: 'bold',
        }}
      >
        {buttonLabel}
      </a>
    </LoginCard>
  );
}

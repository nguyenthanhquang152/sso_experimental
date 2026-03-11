export function ServerSideLogin() {
  return (
    <div style={{
      border: '1px solid #e0e0e0',
      borderRadius: '8px',
      padding: '24px',
      maxWidth: '400px',
    }}>
      <h2>Server-Side Flow</h2>
      <p style={{ color: '#666', fontSize: '14px' }}>
        The browser redirects to Google via the Spring Boot backend.
        Spring Security handles the entire OAuth2 authorization code exchange.
      </p>
      <a
        href="/api/oauth2/authorization/google"
        style={{
          display: 'inline-block',
          padding: '10px 24px',
          backgroundColor: '#4285f4',
          color: 'white',
          textDecoration: 'none',
          borderRadius: '4px',
          fontWeight: 'bold',
        }}
      >
        Sign in with Google (Server-Side)
      </a>
    </div>
  );
}

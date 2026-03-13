import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { HomePage } from './pages/HomePage';
import { DashboardPage } from './pages/DashboardPage';
import { useProviderConfig } from './hooks/useProviderConfig';

function App() {
  const { providerConfig } = useProviderConfig();

  const routes = (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage providerConfig={providerConfig} />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );

  if (!providerConfig.google.clientId) {
    return routes;
  }

  return (
    <GoogleOAuthProvider clientId={providerConfig.google.clientId}>
      {routes}
    </GoogleOAuthProvider>
  );
}

export default App;

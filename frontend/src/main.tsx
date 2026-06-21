import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { AuthProvider } from './auth/AuthProvider';
import { nonProdAuthConfig } from './auth/authConfig';
import './styles/globals.css';

const root = document.getElementById('root');
if (!root) throw new Error('Root element #root not found');

if (nonProdAuthConfig.enableVercelAnalytics || nonProdAuthConfig.enableVercelSpeedInsights) {
  window.dispatchEvent(new CustomEvent('stocktracker:telemetry-enabled'));
}

createRoot(root).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);

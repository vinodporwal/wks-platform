import React, { useState } from 'react';
import { Button, Dialog, DialogTitle, DialogContent, DialogActions, Typography, Box } from '@mui/material';
import Config from '../consts';

const SessionHealthCheck = ({ keycloak }) => {
  const [open, setOpen] = useState(false);
  const [healthData, setHealthData] = useState(null);
  const [loading, setLoading] = useState(false);

  const checkSessionHealth = async () => {
    setLoading(true);
    try {
      // Check header sizes with authentication
      const authResponse = await fetch(`${Config.CaseEngineUrl}/debug/auth-headers`, {
        headers: {
          Authorization: `Bearer ${keycloak.token}`,
        },
      });

      if (authResponse.ok) {
        const authData = await authResponse.json();
        
        // Get browser-side information
        const cookieCount = document.cookie.split(';').filter(c => c.trim()).length;
        const cookieSize = document.cookie.length;
        const tokenSize = keycloak.token ? keycloak.token.length : 0;
        
        const browserData = {
          cookieCount,
          cookieSize,
          tokenSize: tokenSize + ' bytes',
          userAgent: navigator.userAgent.length + ' bytes',
          appVersion: process.env.REACT_APP_VERSION || 'Unknown',
          isInIframe: window.self !== window.top,
        };

        setHealthData({
          server: authData,
          browser: browserData,
          recommendations: generateRecommendations(authData, browserData),
        });
      } else {
        setHealthData({
          error: `Failed to fetch session health: ${authResponse.status} ${authResponse.statusText}`,
        });
      }
    } catch (error) {
      setHealthData({
        error: `Network error: ${error.message}`,
      });
    }
    setLoading(false);
  };

  const generateRecommendations = (serverData, browserData) => {
    const recommendations = [];
    
    if (serverData.status === 'EXCEEDS LIMIT') {
      recommendations.push({
        type: 'critical',
        message: 'Header size exceeds server limits. This may cause 400 errors.',
        action: 'Clear browser cookies and refresh the page.',
      });
    }
    
    if (browserData.cookieCount > 20) {
      recommendations.push({
        type: 'warning',
        message: `High cookie count detected (${browserData.cookieCount}).`,
        action: 'Consider clearing browser cookies for this domain.',
      });
    }
    
    if (browserData.cookieSize > 4096) {
      recommendations.push({
        type: 'warning',
        message: `Large cookie size detected (${browserData.cookieSize} bytes).`,
        action: 'Clear cookies to reduce header size.',
      });
    }
    
    if (browserData.isInIframe) {
      recommendations.push({
        type: 'info',
        message: 'Running in iframe mode (APM embedded).',
        action: 'This may cause larger header sizes due to additional cookies.',
      });
    }
    
    return recommendations;
  };

  const clearCookiesAndRefresh = () => {
    // Only clear problematic cookies, not all cookies
    const problematicCookies = [
      'apm_session', 'apm_token', 'apm_auth', 'apm_sso',
      'sso_session', 'sso_token', 'sso_auth', 'sso_state',
      'KC_RESTART', 'AUTH_SESSION_ID', 'AUTH_SESSION_ID_LEGACY',
      'connect.sid', 'session_state', 'legacy_session'
    ];
    
    document.cookie.split(";").forEach(cookie => {
      const eqPos = cookie.indexOf("=");
      const name = eqPos > -1 ? cookie.substr(0, eqPos).trim() : cookie.trim();
      
      // Only clear cookies that are known to cause header bloat
      const shouldClear = problematicCookies.includes(name) ||
                         name.toLowerCase().includes('apm_') ||
                         name.toLowerCase().includes('_sso_') ||
                         (name.toLowerCase().includes('session') && name.length > 20);
      
      if (shouldClear) {
        document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
        console.log(`Cleared problematic cookie: ${name}`);
      }
    });
    
    // Only clear version to trigger selective cleanup on next load
    localStorage.removeItem('wks_app_version');
    
    // Refresh page
    window.location.reload();
  };

  const handleOpen = () => {
    setOpen(true);
    checkSessionHealth();
  };

  return (
    <>
      <Button 
        variant="outlined" 
        size="small" 
        onClick={handleOpen}
        sx={{ ml: 1 }}
      >
        Session Health
      </Button>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Session Health Check</DialogTitle>
        <DialogContent>
          {loading && <Typography>Checking session health...</Typography>}
          
          {healthData?.error && (
            <Box sx={{ color: 'error.main', mb: 2 }}>
              <Typography variant="h6">Error</Typography>
              <Typography>{healthData.error}</Typography>
            </Box>
          )}

          {healthData?.server && (
            <>
              <Box sx={{ mb: 3 }}>
                <Typography variant="h6" gutterBottom>Server Information</Typography>
                <Typography>Total Header Size: <strong>{healthData.server.totalHeaderSize}</strong></Typography>
                <Typography>Status: <strong style={{ color: healthData.server.status === 'OK' ? 'green' : 'red' }}>
                  {healthData.server.status}
                </strong></Typography>
                <Typography>Max Allowed: {healthData.server.maxAllowed}</Typography>
                {healthData.server.authTokenSize && (
                  <Typography>Auth Token Size: {healthData.server.authTokenSize}</Typography>
                )}
                {healthData.server.cookieSize && (
                  <Typography>Cookie Header Size: {healthData.server.cookieSize}</Typography>
                )}
              </Box>

              <Box sx={{ mb: 3 }}>
                <Typography variant="h6" gutterBottom>Browser Information</Typography>
                <Typography>App Version: {healthData.browser.appVersion}</Typography>
                <Typography>Cookie Count: {healthData.browser.cookieCount}</Typography>
                <Typography>Total Cookie Size: {healthData.browser.cookieSize} bytes</Typography>
                <Typography>Token Size: {healthData.browser.tokenSize}</Typography>
                <Typography>In Iframe: {healthData.browser.isInIframe ? 'Yes' : 'No'}</Typography>
              </Box>

              {healthData.recommendations.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="h6" gutterBottom>Recommendations</Typography>
                  {healthData.recommendations.map((rec, index) => (
                    <Box 
                      key={index}
                      sx={{ 
                        mb: 1, 
                        p: 2, 
                        border: 1, 
                        borderColor: rec.type === 'critical' ? 'error.main' : 
                                    rec.type === 'warning' ? 'warning.main' : 'info.main',
                        borderRadius: 1,
                        bgcolor: rec.type === 'critical' ? 'error.light' : 
                                rec.type === 'warning' ? 'warning.light' : 'info.light',
                        opacity: 0.8
                      }}
                    >
                      <Typography variant="body2" sx={{ fontWeight: 'bold', mb: 1 }}>
                        {rec.message}
                      </Typography>
                      <Typography variant="body2">{rec.action}</Typography>
                    </Box>
                  ))}
                </Box>
              )}
            </>
          )}
        </DialogContent>
        <DialogActions>
          {healthData?.recommendations?.some(r => r.type === 'critical' || r.type === 'warning') && (
            <Button onClick={clearCookiesAndRefresh} variant="contained" color="warning">
              Clear Cookies & Refresh
            </Button>
          )}
          <Button onClick={() => setOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default SessionHealthCheck;
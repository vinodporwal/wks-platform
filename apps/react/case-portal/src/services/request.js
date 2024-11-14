/* eslint-disable no-unused-vars */
export function json(keycloak, resp) {
  if (resp.status === 401) {
    handleTokenRefresh(keycloak, resp)
    return Promise.reject(resp)
  }

  if (resp.ok) {
    return resp.json()
  }

  return Promise.resolve(resp)
}

export function nop(keycloak, resp) {
  if (resp.status === 401) {
    handleTokenRefresh(keycloak, resp)
    return Promise.reject(resp)
  }

  return resp
}

function handleTokenRefresh(keycloak, resp) {
  if (keycloak.isTokenExpired()) {
    console.warn('Token expired. Attempting to refresh...')

    // Attempt to refresh the token
    keycloak
      .updateToken(70)
      .then((refreshed) => {
        if (refreshed) {
          console.info('Token refreshed successfully.')
          // Optionally re-attempt the failed API call here if needed
        } else {
          console.warn('Token still valid, no refresh needed.')
        }
      })
      .catch(() => {
        console.error('Failed to refresh token')
        // Optionally log out the user if the refresh failed
        // keycloak.logout({ redirectUri: window.location.origin });
      })
  }
}

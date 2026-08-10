import Config from '../consts'

/**
 * Service for Role-Based User Screen Mapping API calls
 */
export const userScreenMappingApiService = {
  /**
   * Fetch screens assigned to user based on Keycloak roles
   * GET /task/user/screen/screen-by-role?verticalId=...&plantId=...
   */
  getUserScreenMappingByRole: async (keycloakSession, verticalId, plantId, userId) => {
    let url = `${Config.CaseEngineUrl}/task/user/screen/screen-by-role?verticalId=${verticalId}&plantId=${plantId}`
    if (userId) {
      url += `&userId=${userId}`
    }

    const token = typeof keycloakSession === 'string' ? keycloakSession : keycloakSession?.token

    const headers = {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    }

    try {
      const resp = await fetch(url, { method: 'GET', headers })
      if (resp.status === 401 && typeof keycloakSession === 'object' && keycloakSession?.logout) {
        keycloakSession.logout()
      }
      return await resp.json()
    } catch (e) {
      console.error('Error fetching screens by role:', e)
      return await Promise.reject(e)
    }
  },
}

export default userScreenMappingApiService

import Config from '../consts'
import { json } from './request'

/**
 * Service for Role Access Management APIs:
 * 1. Role Creation: POST /task/users/roles
 * 2. Roles List: GET /task/users/roles?q=&page=&size=
 * 3. Role Retrieval by User: GET /task/users/{userId}/roles
 * 4. Role Assignment: POST /task/users/roles/assign
 */

const handleResponse = async (keycloak, response) => {
  if (response.status === 401) {
    if (keycloak && typeof keycloak.logout === 'function') {
      keycloak.logout()
    }
    return Promise.reject(new Error('Unauthorized'))
  }
  if (response.status === 409) {
    return Promise.reject(new Error('Role already exists (409)'))
  }
  if (!response.ok) {
    const errText = await response.text()
    return Promise.reject(new Error(errText || `HTTP Error ${response.status}`))
  }
  return response.json()
}

export const roleAccessApiService = {
  /**
   * 1. Create a New Role
   * POST /task/users/roles
   * Body: { name, description }
   */
  createRole: async (keycloak, { name, description }) => {
    const url = `${Config.CaseEngineUrl}/task/users/roles`
    const headers = {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: keycloak?.token ? `Bearer ${keycloak.token}` : '',
    }

    const payload = {
      name: name,
      description: description || '',
    }

    try {
      const resp = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
      })
      return await handleResponse(keycloak, resp)
    } catch (e) {
      console.error('API Error in createRole:', e)
      return Promise.reject(e)
    }
  },

  /**
   * 2. Roles List API
   * GET /task/users/roles?q=&page=&size=
   */
  getRoles: async (keycloak, { q = '', page = 1, size = 20 } = {}) => {
    const params = new URLSearchParams()
    if (q) params.append('q', q)
    if (page) params.append('page', page)
    if (size) params.append('size', size)

    const url = `${Config.CaseEngineUrl}/task/users/roles?${params.toString()}`
    const headers = {
      Accept: 'application/json',
      Authorization: keycloak?.token ? `Bearer ${keycloak.token}` : '',
    }

    try {
      const resp = await fetch(url, { method: 'GET', headers })
      return await handleResponse(keycloak, resp)
    } catch (e) {
      console.error('API Error in getRoles:', e)
      return Promise.reject(e)
    }
  },

  /**
   * 3. Role Retrieval (by user)
   * GET /task/users/{userId}/roles
   */
  getUserRoles: async (keycloak, userId) => {
    const url = `${Config.CaseEngineUrl}/task/users/${encodeURIComponent(userId)}/roles`
    const headers = {
      Accept: 'application/json',
      Authorization: keycloak?.token ? `Bearer ${keycloak.token}` : '',
    }

    try {
      const resp = await fetch(url, { method: 'GET', headers })
      return await handleResponse(keycloak, resp)
    } catch (e) {
      console.error(`API Error in getUserRoles for ${userId}:`, e)
      return Promise.reject(e)
    }
  },

  /**
   * 4. Roles Assign API (Bulk / Many-to-Many)
   * POST /task/users/roles/assign
   * Body: { assignments: [ { userId, roles: [...] }, ... ] }
   */
  assignRoles: async (keycloak, assignments) => {
    const url = `${Config.CaseEngineUrl}/task/users/roles/assign`
    const headers = {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: keycloak?.token ? `Bearer ${keycloak.token}` : '',
    }

    const payload = {
      assignments: Array.isArray(assignments) ? assignments : [assignments],
    }

    try {
      const resp = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
      })
      return await handleResponse(keycloak, resp)
    } catch (e) {
      console.error('API Error in assignRoles:', e)
      return Promise.reject(e)
    }
  },

  /**
   * 5. Delete Role API
   * DELETE /task/users/roles/{roleName}
   */
  deleteRole: async (keycloak, roleName) => {
    const url = `${Config.CaseEngineUrl}/task/users/roles/${encodeURIComponent(roleName)}`
    const headers = {
      Accept: 'application/json',
      Authorization: keycloak?.token ? `Bearer ${keycloak.token}` : '',
    }

    try {
      const resp = await fetch(url, { method: 'DELETE', headers })
      return await handleResponse(keycloak, resp)
    } catch (e) {
      console.error(`API Error in deleteRole for ${roleName}:`, e)
      return Promise.reject(e)
    }
  },

  /**
   * 6. Unassign Role from User API
   * DELETE /task/users/{userId}/roles/{roleName}
   */
  unassignRoleFromUser: async (keycloak, userId, roleName) => {
    const url = `${Config.CaseEngineUrl}/task/users/${encodeURIComponent(userId)}/roles/${encodeURIComponent(roleName)}`
    const headers = {
      Accept: 'application/json',
      Authorization: keycloak?.token ? `Bearer ${keycloak.token}` : '',
    }

    try {
      const resp = await fetch(url, { method: 'DELETE', headers })
      return await handleResponse(keycloak, resp)
    } catch (e) {
      console.error(
        `API Error in unassignRoleFromUser for ${userId} - ${roleName}:`,
        e,
      )
      return Promise.reject(e)
    }
  },

  /**
   * 7. Get Users by Roles API (Union query with pagination)
   * POST /task/users/by-roles
   * Body: { roles: ["cts_head", "plant_manager"], page: 1, size: 20 }
   */
  getUsersByRoles: async (keycloak, { roles = [], page = 1, size = 20 } = {}) => {
    const url = `${Config.CaseEngineUrl}/task/users/by-roles`
    const headers = {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: keycloak?.token ? `Bearer ${keycloak.token}` : '',
    }

    const payload = {
      roles: Array.isArray(roles) ? roles : [roles],
      page,
      size,
    }

    try {
      const resp = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
      })
      return await handleResponse(keycloak, resp)
    } catch (e) {
      console.error('API Error in getUsersByRoles:', e)
      return Promise.reject(e)
    }
  },
}

export default roleAccessApiService

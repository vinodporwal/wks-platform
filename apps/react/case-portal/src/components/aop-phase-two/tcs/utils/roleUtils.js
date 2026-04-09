// Role definitions and utilities for TCS workflow

export const ROLES = {
  CTS_HEAD: 'cts_head',
  EPS_HEAD: 'eps_head',
  EPS_ENGINEER: 'eps_engineer',
  PLANT_MANAGER: 'plant_manager',
  CTS_TECH_MANAGER: 'cts_tect_manager',
  CLUSTER_HEAD: 'cluster_head',
}

/**
 * Extract user's primary workflow role from keycloak roles based on priority
 * Priority order: cts_head > eps_head > eps_engineer > plant_manager > cts_admin
 * @param {Array} keycloakRoles - Array of roles from keycloak
 * @returns {string|null} - Primary workflow role or null
 */
export const getUserRole = (keycloakRoles = []) => {
  if (!Array.isArray(keycloakRoles) || keycloakRoles.length === 0) {
    return null
  }

  const rolePriority = [
    ROLES.CLUSTER_HEAD,
    ROLES.EPS_HEAD,
    ROLES.CTS_HEAD,
    ROLES.EPS_ENGINEER,
    ROLES.PLANT_MANAGER,
    ROLES.CTS_TECH_MANAGER,
  ]

  for (const role of rolePriority) {
    if (keycloakRoles.includes(role)) {
      return role
    }
  }

  return null
}

/**
 * Get user-friendly label for a role
 * @param {string} role - Role constant from ROLES
 * @returns {string} - User-friendly role label
 */
export const getRoleLabel = (role) => {
  switch (role) {
    case ROLES.PLANT_MANAGER:
      return 'CTS Engineer'
    case ROLES.EPS_ENGINEER:
      return 'AOM'
    case ROLES.EPS_HEAD:
      return 'EPS Head'
    case ROLES.CLUSTER_HEAD:
      return 'R&M Cluster Head'
    default:
      return 'Unknown Role'
  }
}

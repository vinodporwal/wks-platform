import Config from 'consts/index'
import { json } from '../../../../services/request'

export const TabAccessApiService = {
  // Tab Configuration APIs (existing)
  getConfigurationTabsMatrix,
  getConfigurationAvailableTabs,

  // ConfigurationType CRUD APIs
  getConfigurationTypeById,
  createConfigurationType,
  updateConfigurationType,
  deleteConfigurationType,

  // ConfigurationAccessMatrix CRUD APIs
  getAllAccessMatrix,
  getAccessMatrixById,
  createAccessMatrix,
  updateAccessMatrix,
  deleteAccessMatrix,

  // Plant/Site/Vertical Hierarchy API
  getPlantSiteVertical,
}

const getHeaders = (keycloak) => ({
  Accept: 'application/json',
  'Content-Type': 'application/json',
  Authorization: `Bearer ${keycloak.token}`,
})

// ===================== || Tab Configuration APIs || ===================== //

/**
 * Get Configuration Tabs Matrix
 * Returns which tabs should be displayed based on plant/site/vertical configuration
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} siteId - Site ID
 * @param {string} verticalId - Vertical ID
 * @param {string} [type] - Optional type filter (e.g. 'TCS', 'OutputReport')
 * @returns {Promise} Configuration tabs matrix data
 */
async function getConfigurationTabsMatrix(
  keycloak,
  plantId,
  year,
  siteId,
  verticalId,
  type,
) {
  const url = `${Config.CaseEngineUrl}/task/access/matrix`
  const headers = getHeaders(keycloak)

  const params = new URLSearchParams({
    plantId: plantId,
    auditYear: year,
    siteId: siteId,
    verticalId: verticalId,
  })

  if (type) {
    params.append('type', type)
  }

  try {
    const resp = await fetch(`${url}?${params}`, {
      method: 'GET',
      headers: headers,
    })

    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }

    return json(keycloak, resp)
  } catch (error) {
    console.error('Error fetching configuration tabs matrix:', error)
    return await Promise.reject(error)
  }
}

/**
 * Get Configuration Available Tabs
 * Returns all possible configuration tabs with their metadata (id, displayName, etc.)
 * @param {Object} keycloak - Keycloak session
 * @returns {Promise} Available configuration types list
 */
async function getConfigurationAvailableTabs(keycloak) {
  const url = `${Config.CaseEngineUrl}/task/configuration-type-data`

  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: getHeaders(keycloak),
    })

    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }

    return json(keycloak, resp)
  } catch (error) {
    console.error('Error fetching configuration available tabs:', error)
    return await Promise.reject(error)
  }
}

// ===================== || ConfigurationType CRUD APIs || ===================== //

async function getConfigurationTypeById(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/configuration-type-data/${id}`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: getHeaders(keycloak),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching configuration type:', e)
    return await Promise.reject(e)
  }
}

async function createConfigurationType(keycloak, dto) {
  const url = `${Config.CaseEngineUrl}/task/configuration-type-data`
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: getHeaders(keycloak),
      body: JSON.stringify(dto),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error creating configuration type:', e)
    return await Promise.reject(e)
  }
}

async function updateConfigurationType(keycloak, id, dto) {
  const url = `${Config.CaseEngineUrl}/task/configuration-type-data/${id}`
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers: getHeaders(keycloak),
      body: JSON.stringify(dto),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error updating configuration type:', e)
    return await Promise.reject(e)
  }
}

async function deleteConfigurationType(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/configuration-type-data/${id}`
  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers: getHeaders(keycloak),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error deleting configuration type:', e)
    return await Promise.reject(e)
  }
}

// ===================== || ConfigurationAccessMatrix CRUD APIs || ===================== //

async function getAllAccessMatrix(keycloak) {
  const url = `${Config.CaseEngineUrl}/task/access/matrix/all`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: getHeaders(keycloak),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching all access matrix:', e)
    return await Promise.reject(e)
  }
}

async function getAccessMatrixById(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/access/matrix/${id}`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: getHeaders(keycloak),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching access matrix by id:', e)
    return await Promise.reject(e)
  }
}

async function createAccessMatrix(keycloak, dto) {
  const url = `${Config.CaseEngineUrl}/task/access/matrix`
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers: getHeaders(keycloak),
      body: JSON.stringify(dto),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error creating access matrix:', e)
    return await Promise.reject(e)
  }
}

async function updateAccessMatrix(keycloak, id, dto) {
  const url = `${Config.CaseEngineUrl}/task/access/matrix/${id}`
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers: getHeaders(keycloak),
      body: JSON.stringify(dto),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error updating access matrix:', e)
    return await Promise.reject(e)
  }
}

async function deleteAccessMatrix(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/access/matrix/${id}`
  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers: getHeaders(keycloak),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error deleting access matrix:', e)
    return await Promise.reject(e)
  }
}

// ===================== || Plant/Site/Vertical Hierarchy API || ===================== //

/**
 * Get Plant/Site/Vertical Hierarchy
 * Returns nested array of verticals -> sites -> plants
 * @param {Object} keycloak - Keycloak session
 * @returns {Promise} Array of { id, name, displayName, sites: [{ id, name, displayName, plants: [...] }] }
 */
async function getPlantSiteVertical(keycloak) {
  const url = `${Config.CaseEngineUrl}/task/plant-site-vertical`
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers: getHeaders(keycloak),
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching plant-site-vertical:', e)
    return await Promise.reject(e)
  }
}

export default TabAccessApiService

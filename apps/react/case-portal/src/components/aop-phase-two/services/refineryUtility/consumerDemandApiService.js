import Config from 'consts/index'
import { json } from 'services/request'

export const ConsumerDemandApiService = {
  getConsumerDemand,
  saveConsumerDemand,
  exportConsumerDemand,
  importConsumerDemand,
}

// ========================|| Consumer Demand APIs ||=====================================//

/**
 * Get Consumer Demand Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string|number} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Consumer demand data
 */
async function getConsumerDemand(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/consumer-demand?year=${year}&plantFKId=${plantId}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching consumer demand data:', e)
    return await Promise.reject(e)
  }
}

/**
 * Save Consumer Demand Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string|number} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {Array} data - Consumer demand data to save
 * @returns {Promise<Object>} Save response
 */
async function saveConsumerDemand(keycloak, plantId, year, data) {
  const url = `${Config.CaseEngineUrl}/task/consumer-demand?year=${year}&plantFKId=${plantId}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error saving consumer demand data:', e)
    return await Promise.reject(e)
  }
}

/**
 * Export Consumer Demand to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string|number} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise<Blob>} Excel file blob
 */
async function exportConsumerDemand(keycloak, plantId, year) {
  const baseUrl = `${Config.CaseEngineUrl}/task/consumer-demand-export`
  const queryParams = new URLSearchParams({
    plantId,
    year,
  })

  const url = `${baseUrl}?${queryParams.toString()}`
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.statusText}`)
    }
    return await resp.blob()
  } catch (e) {
    console.error('Error exporting consumer demand data:', e)
    return await Promise.reject(e)
  }
}

/**
 * Import Consumer Demand from Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string|number} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {File} file - Excel file to import
 * @returns {Promise<Array>} Imported data
 */
async function importConsumerDemand(keycloak, plantId, year, file) {
  const baseUrl = `${Config.CaseEngineUrl}/task/consumer-demand-import`
  const formData = new FormData()
  formData.append('file', file)
  formData.append('plantId', plantId)
  formData.append('year', year)

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(baseUrl, {
      method: 'POST',
      headers,
      body: formData,
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error importing consumer demand data:', e)
    return await Promise.reject(e)
  }
}

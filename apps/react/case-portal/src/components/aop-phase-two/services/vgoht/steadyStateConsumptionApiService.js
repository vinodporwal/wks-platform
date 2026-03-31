import Config from 'consts/index'
import { json } from 'services/request'

export const SteadyStateConsumptionApiService = {
  getSteadyStateConsumption,
  saveSteadyStateConsumption,
  exportSteadyStateConsumption,
  importSteadyStateConsumption,
  calculateSteadyStateConsumption,
}

// ========================|| Steady State Consumption APIs ||=====================================//

/**
 * Get Steady State Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Steady state consumption data
 */
async function getSteadyStateConsumption(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/vgoht/norms-basis?year=${year}&plantFKId=${plantId}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Save Steady State Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {String} plantId - Plant ID (UUID)
 * @param {String} year - AOP Year (e.g., "2026-27")
 * @param {Date} startDate - Start date from configuration
 * @param {Date} endDate - End date from configuration
 * @param {Array} data - Steady state consumption data to save
 * @returns {Promise<Object>} Save response
 */
async function saveSteadyStateConsumption(
  keycloak,
  plantId,
  year,
  startDate,
  endDate,
  data,
) {
  // Format dates to YYYY-MM-DD
  const formatDate = (date) => {
    if (!date) return null
    const d = new Date(date)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  const periodFrom = formatDate(startDate)
  const periodTo = formatDate(endDate)

  const baseUrl = `${Config.CaseEngineUrl}/task/vgoht/norms-basis`
  const queryParams = new URLSearchParams({
    year,
    plantFKId: plantId,
    periodFrom,
    periodTo,
  })
  const url = `${baseUrl}?${queryParams.toString()}`
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

    // Check for HTTP errors before parsing response
    if (!resp.ok) {
      const errorText = await resp.text()
      throw new Error(`HTTP ${resp.status}: ${errorText || resp.statusText}`)
    }

    return json(keycloak, resp)
  } catch (e) {
    console.error('Error in saveSteadyStateConsumption:', e)
    return await Promise.reject(e)
  }
}

/**
 * Export Steady State Consumption to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {Number} plantId - Plant ID
 * @param {Number} year - AOP Year
 * @returns {Promise<Blob>} Excel file blob
 */
async function exportSteadyStateConsumption(keycloak, plantId, year) {
  const baseUrl = `${Config.CaseEngineUrl}/task/vgoht/steady-state-consumption/export`
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
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Import Steady State Consumption from Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {Number} plantId - Plant ID
 * @param {Number} year - AOP Year
 * @param {File} file - Excel file to import
 * @returns {Promise<Array>} Imported data
 */
async function importSteadyStateConsumption(keycloak, plantId, year, file) {
  const baseUrl = `${Config.CaseEngineUrl}/task/vgoht/steady-state-consumption/import`
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
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Calculate Steady State Consumption
 * @param {Object} keycloak - Keycloak session object
 * @param {String} plantId - Plant ID (UUID)
 * @param {String} year - AOP Year (e.g., "2026-27")
 * @param {Date} startDate - Period start date
 * @param {Date} endDate - Period end date
 * @returns {Promise<Array>} Calculated data
 */
async function calculateSteadyStateConsumption(
  keycloak,
  plantId,
  year,
  startDate,
  endDate,
) {
  // Format dates to YYYY-MM-DD
  const formatDate = (date) => {
    if (!date) return null
    const d = new Date(date)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  const periodFrom = formatDate(startDate)
  const periodTo = formatDate(endDate)

  const baseUrl = `${Config.CaseEngineUrl}/task/vgoht/norms-basis/calculate`
  const queryParams = new URLSearchParams({
    year,
    plantFKId: plantId,
    periodFrom,
    periodTo,
  })

  const url = `${baseUrl}?${queryParams.toString()}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

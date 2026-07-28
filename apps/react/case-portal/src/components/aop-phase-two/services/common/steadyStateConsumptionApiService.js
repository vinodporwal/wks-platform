import Config from 'consts/index'
import { json } from 'services/request'

export const SteadyStateConsumptionApiService = {
  getSteadyStateConsumption,
  saveSteadyStateConsumption,
  exportSteadyStateConsumption,
  importSteadyStateConsumption,
  calculateSteadyStateConsumption,
  calculateCommonSteadyStateConsumption,
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
  // Construct URL based on presence of gradeId
  const baseUrl = `${Config.CaseEngineUrl}/task/steady-state-norms`
  const queryParams = new URLSearchParams({
    year,
    plantId,
  })

  const url = `${baseUrl}?${queryParams.toString()}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Save Steady State Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {Number} plantId - Plant ID
 * @param {Number} year - AOP Year
 * @param {Array} data - Steady state consumption data to save
 * @returns {Promise<Object>} Save response
 */
async function saveSteadyStateConsumption(keycloak, plantId, year, data) {
  const baseUrl = `${Config.CaseEngineUrl}/task/steady-state-norms`
  const queryParams = new URLSearchParams({
    plantId,
    year,
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
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Export Steady State Consumption to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {Number} plantId - Plant ID
 * @param {Number} year - AOP Year
 * @param {String} mode - Optional mode parameter
 * @param {String} gradeId - Optional grade ID parameter
 * @returns {Promise<Blob>} Excel file blob
 */
async function exportSteadyStateConsumption(
  keycloak,
  plantId,
  year,
  mode,
  gradeId,
) {
  const baseUrl = `${Config.CaseEngineUrl}/task/steady-state-norms-export`
  const queryParams = new URLSearchParams({
    plantId,
    year,
  })

  if (mode) {
    queryParams.append('mode', mode)
  }
  if (gradeId) {
    queryParams.append('gradeId', gradeId)
  }

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
 * @param {String} mode - Optional mode parameter
 * @param {String} gradeId - Optional grade ID parameter
 * @returns {Promise<Array>} Imported data
 */
async function importSteadyStateConsumption(
  keycloak,
  plantId,
  year,
  file,
  mode,
  gradeId,
) {
  const baseUrl = `${Config.CaseEngineUrl}/task/steady-state-norms-import`
  const formData = new FormData()
  formData.append('file', file)
  formData.append('plantId', plantId)
  formData.append('year', year)

  if (mode) {
    formData.append('mode', mode)
  }
  if (gradeId) {
    formData.append('gradeId', gradeId)
  }

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
 * @param {Number} plantId - Plant ID
 * @param {Number} year - AOP Year
 * @returns {Promise<Array>} Calculated data
 */
async function calculateSteadyStateConsumption(keycloak, plantId, year) {
  const baseUrl = `${Config.CaseEngineUrl}/task/crude/calculate-steady-state-norms`
  const queryParams = new URLSearchParams({
    year,
    plantId,
  })

  const url = `${baseUrl}?${queryParams.toString()}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Common Calculate Steady State Consumption
 * @param {Object} keycloak - Keycloak session object
 * @param {Number} plantId - Plant ID
 * @param {Number} year - AOP Year
 * @returns {Promise<Array>} Calculated data
 */
async function calculateCommonSteadyStateConsumption(keycloak, plantId, year) {
  const baseUrl = `${Config.CaseEngineUrl}/task/calculate-steady-state-norms`
  const queryParams = new URLSearchParams({
    year,
    plantId,
  })

  const url = `${baseUrl}?${queryParams.toString()}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

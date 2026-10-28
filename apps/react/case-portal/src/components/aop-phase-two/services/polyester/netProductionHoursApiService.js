import Config from 'consts/index'
import { json } from 'services/request'

export const NetProductionHoursApiService = {
  getNetProductionHours,
  saveNetProductionHours,
  exportNetProductionHours,
}

// ========================|| Net Production Hours APIs ||=====================================//

/**
 * Get Net Production Hours Data (PE vertical / NMD site uses maintenance-details endpoint)
 * @param {Object} keycloak - Keycloak session object
 * @param {string} PLANT_ID - Plant ID
 * @param {string} AOP_YEAR - AOP Year
 * @returns {Promise} Net production hours data
 */
async function getNetProductionHours(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/maintenance-details?year=${AOP_YEAR}&plantId=${PLANT_ID}`
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
 * Save Net Production Hours Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} PLANT_ID - Plant ID
 * @param {string} AOP_YEAR - AOP Year
 * @param {Array} payload - Net production hours data to save
 * @returns {Promise} Save response
 */
async function saveNetProductionHours(keycloak, PLANT_ID, AOP_YEAR, payload) {
  const url = `${Config.CaseEngineUrl}/task/maintenance-details?year=${AOP_YEAR}&plantId=${PLANT_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Export Net Production Hours to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} PLANT_ID - Plant ID
 * @param {string} AOP_YEAR - AOP Year
 * @param {string} EXCEL_NAME - Excel file name (without .xlsx)
 */
async function exportNetProductionHours(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  EXCEL_NAME,
) {
  const url = `${Config.CaseEngineUrl}/task/maintenance-details-export?year=${encodeURIComponent(AOP_YEAR)}&plantId=${encodeURIComponent(PLANT_ID)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_NAME}`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Net Production Hours Excel:', e)
    return Promise.reject(e)
  }
}

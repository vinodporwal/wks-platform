import Config from 'consts/index'
import { json } from 'services/request'

export const OverallAopConsumptionApiService = {
  getGrades,
  getOverallAopConsumption,
  saveOverallAopConsumption,
  calculateOverallAopConsumption,
  exportOverallAopConsumption,
}

// ========================|| Overall AOP Consumption APIs ||=====================================//

/**
 * Get Grades Dropdown for Polyester/PE/NMD
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Grades dropdown list
 */
async function getGrades(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/consumption-aop/grades?year=${year}&plantId=${plantId}`
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
 * Get Overall AOP Consumption Data (grade-based)
 * @param {Object} keycloak - Keycloak session object
 * @param {string} gradeId - Selected Grade ID
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Overall AOP consumption data
 */
async function getOverallAopConsumption(keycloak, gradeId, plantId, year) {
  const baseUrl = `${Config.CaseEngineUrl}/task/overall-consumption`
  const queryParams = new URLSearchParams({
    plantId,
    year,
  })
  if (gradeId) {
    queryParams.append('gradeId', gradeId)
  }
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
 * Save Overall AOP Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {Array} payload - Body containing shutdown / consumption data to save
 * @returns {Promise} Save response
 */
async function saveOverallAopConsumption(keycloak, plantId, payload) {
  const url = `${Config.CaseEngineUrl}/task/overall-consumption`
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
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Calculate Overall AOP Consumption
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Calculated data
 */
async function calculateOverallAopConsumption(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/calculate-overall-consumption?year=${year}&plantId=${plantId}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return await resp.json()
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Export Overall AOP Consumption to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} excelExportTitle - File title prefix
 * @param {string} screenName - Screen title suffix
 * @returns {Promise}
 */
async function exportOverallAopConsumption(
  keycloak,
  plantId,
  year,
  excelExportTitle,
  screenName,
) {
  const url = `${Config.CaseEngineUrl}/task/overall-consumption-export?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
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
    a.download = `${excelExportTitle}_${screenName}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

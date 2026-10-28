import Config from 'consts/index'
import { json } from 'services/request'

export const MonthwiseProductionPlanApiService = {
  getMonthwiseProductionPlan,
  saveMonthwiseProductionPlan,
  exportMonthwiseProductionPlan,
  importMonthwiseProductionPlan,
  calculateMonthwiseProductionPlan,
}

// ========================|| Monthwise Production Plan APIs  ||=====================================//

/**
 * Get Monthwise Production Plan Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} PLANT_ID - Plant ID
 * @param {string} AOP_YEAR - AOP Year
 * @returns {Promise} Monthly production data (response.data.aopDTOList)
 */
async function getMonthwiseProductionPlan(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/monthly-production?plantId=${PLANT_ID}&year=${AOP_YEAR}&type=Production`
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
 * Save Monthwise Production Plan Data
 * @param {Object} keycloak - Keycloak session object
 * @param {Array} payload - Production plan data to save
 * @returns {Promise} Save response
 */
async function saveMonthwiseProductionPlan(keycloak, payload) {
  const url = `${Config.CaseEngineUrl}/task/monthly-production`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers,
      body: JSON.stringify(payload),
    })
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
 * Export Monthwise Production Plan to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} PLANT_ID - Plant ID
 * @param {string} AOP_YEAR - AOP Year
 * @param {string} EXCEL_NAME - Excel file name (without .xlsx)
 */
async function exportMonthwiseProductionPlan(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  EXCEL_NAME,
) {
  const url = `${Config.CaseEngineUrl}/task/monthly-production-export?year=${encodeURIComponent(AOP_YEAR)}&plantId=${encodeURIComponent(PLANT_ID)}&type=Production`
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
    console.error('Error exporting Monthwise Production Plan Excel:', e)
    return Promise.reject(e)
  }
}

/**
 * Import Monthwise Production Plan from Excel
 */
async function importMonthwiseProductionPlan(keycloak, plantId, year, file) {
  const url = `${Config.CaseEngineUrl}/task/staple/monthwise-production-plan-import`
  const formData = new FormData()
  formData.append('file', file)
  formData.append('plantId', plantId)
  formData.append('aopYear', year)
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
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
 * Calculate Monthwise Production Plan
 * @param {Object} keycloak - Keycloak session object
 * @param {string} PLANT_ID - Plant ID
 * @param {string} AOP_YEAR - AOP Year
 */
async function calculateMonthwiseProductionPlan(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/calculate-monthly-production?year=${AOP_YEAR}&plantId=${PLANT_ID}`
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
    console.error('Error calculating monthwise production plan:', e)
    return Promise.reject(e)
  }
}

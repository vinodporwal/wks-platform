import Config from 'consts/index'

import { ImportExportApiService } from '../common/importExportApiService'
import { json } from 'services/request'

export const OverallAopConsumptionApiService = {
  getOverallAopConsumption,
  calculateOverallAopConsumption,
}

// ========================|| Overall AOP Consumption APIs ||=====================================//

/**
 * Get Overall AOP Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} gradeId - Grade ID (optional)
 * @returns {Promise} Overall AOP consumption data
 */
async function getOverallAopConsumption(keycloak, plantId, year, gradeId) {
  let url = `${Config.CaseEngineUrl}/task/overall-consumption?plantId=${plantId}&year=${year}`
  if (gradeId) {
    url += `&gradeId=${gradeId}`
  }
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
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
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

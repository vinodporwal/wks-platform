import Config from 'consts/index'
import { json } from 'services/request'

export const JwOverallAopConsumptionApiService = {
  getJwOverallAopConsumption,
  saveJwOverallAopConsumption,
  calculateJwOverallAopConsumption,
}

// ========================|| JW Overall AOP Consumption APIs ||=====================================//

/**
 * Get JW Overall AOP Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} JW Overall AOP consumption data
 */
async function getJwOverallAopConsumption(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/jw-overall-aop-consumption?plantId=${plantId}&year=${year}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak?.token}`,
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
 * Save JW Overall AOP Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {Array} payload - Payload data to save
 * @param {string} year - AOP Year
 * @returns {Promise} Response data
 */
async function saveJwOverallAopConsumption(keycloak, payload, year) {
  const url = `${Config.CaseEngineUrl}/task/jw-overall-aop-consumption?year=${year}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak?.token}`,
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
 * Calculate JW Overall AOP Consumption
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Calculated data
 */
async function calculateJwOverallAopConsumption(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/calculate-jw-overall-aop-consumption?year=${year}&plantId=${plantId}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak?.token}`,
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

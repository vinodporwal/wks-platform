import Config from 'consts/index'
import { json } from 'services/request'

export const JwUnitApiService = {
  getJwUnitData,
  saveJwUnitData,
}

// ========================|| JW Unit APIs ||=====================================//

/**
 * Get JW Unit Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} siteId - Site ID
 * @param {string} year - AOP Year
 * @returns {Promise} JW Unit data
 */
async function getJwUnitData(keycloak, siteId, year) {
  const url = `${Config.CaseEngineUrl}/task/jw-unit?siteId=${encodeURIComponent(siteId || '')}&aopYear=${encodeURIComponent(year || '')}`
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
 * Save JW Unit Data
 * Rule: If normParameterFkId + siteFkId + aopYear exists update else insert
 * @param {Object} keycloak - Keycloak session object
 * @param {Array} payload - Payload data list
 * @param {string} year - AOP Year
 * @returns {Promise} Save response
 */
async function saveJwUnitData(keycloak, payload, year) {
  const url = `${Config.CaseEngineUrl}/task/jw-unit?aopYear=${encodeURIComponent(year || '')}`
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

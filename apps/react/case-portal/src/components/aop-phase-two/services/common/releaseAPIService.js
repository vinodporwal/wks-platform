import Config from 'consts/index'
import { json } from '../../../../services/request'

export const ReleaseAPIService = {
  // AOP Approval Flow Release APIs
  getReleaseAOPStatus,
  releaseAOPReport,
}

// ===================== || AOP Release APIs || ===================== //

/**
 * Get Release AOP Status
 * Checks whether the AOP report has already been released for the given plant and year.
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Release status data
 */
async function getReleaseAOPStatus(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/release-aop?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`

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

    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching release AOP status:', e)
    return await Promise.reject(e)
  }
}

/**
 * Release AOP Report
 * Submits the AOP report for release for the given plant and year.
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Release confirmation data
 */
async function releaseAOPReport(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/release-aop?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
    })

    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }

    return json(keycloak, resp)
  } catch (e) {
    console.error('Error releasing AOP report:', e)
    return await Promise.reject(e)
  }
}

export default ReleaseAPIService

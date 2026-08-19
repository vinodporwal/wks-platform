import Config from 'consts/index'
import { json } from '../../../../services/request'

export const ReleaseAPIService = {
  // AOP Approval Flow Release APIs
  getReleaseAOPStatus,
  releaseAOPReport,
  ensureReleaseIfNotReleased,
  deleteReleaseAOPByPlantAndYear,
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

/**
 * Ensure AOP Report is released (checks first, only releases if not already released)
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 */
async function ensureReleaseIfNotReleased(keycloak, plantId, year) {
  if (!plantId || !year) return false
  try {
    const statusRes = await getReleaseAOPStatus(keycloak, plantId, year)
    const isAlreadyReleased =
      statusRes &&
      (statusRes.isReleased === 1 ||
        statusRes.isReleased === true ||
        statusRes.status === 'RELEASED' ||
        (Array.isArray(statusRes) && statusRes.length > 0) ||
        (statusRes.data &&
          Array.isArray(statusRes.data) &&
          statusRes.data.length > 0))

    if (!isAlreadyReleased) {
      await releaseAOPReport(keycloak, plantId, year)
      return true
    }
    return false
  } catch (e) {
    console.error('Error in ensureReleaseIfNotReleased, attempting release:', e)
    try {
      await releaseAOPReport(keycloak, plantId, year)
      return true
    } catch (relErr) {
      console.error('Error calling releaseAOPReport fallback:', relErr)
      return false
    }
  }
}

/**
 * Delete Release AOP by Plant and Year
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 */
async function deleteReleaseAOPByPlantAndYear(keycloak, plantId, year) {
  try {
    const statusRes = await getReleaseAOPStatus(keycloak, plantId, year)
    const list =
      statusRes?.data || (Array.isArray(statusRes) ? statusRes : [])
    for (const item of list) {
      const id = item?.id || item?.Id
      if (id) {
        const url = `${Config.CaseEngineUrl}/task/release-aop?id=${encodeURIComponent(id)}`
        const headers = {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${keycloak.token}`,
        }
        await fetch(url, { method: 'DELETE', headers })
      }
    }
  } catch (e) {
    console.error('Error deleting release AOP by plant and year:', e)
  }
}

export default ReleaseAPIService

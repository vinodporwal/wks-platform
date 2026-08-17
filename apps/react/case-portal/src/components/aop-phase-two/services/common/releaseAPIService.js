import Config from 'consts/index'
import { json } from '../../../../services/request'

export const ReleaseAPIService = {
  // AOP Approval Flow Release APIs
  getReleaseAOPStatus,
  releaseAOPReport,
  deleteReleaseAOP,
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
 * Delete Release AOP
 * Deletes the released AOP record for the given release ID.
 * @param {Object} keycloak - Keycloak session
 * @param {string} id - Release Record ID
 * @returns {Promise}
 */
async function deleteReleaseAOP(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/release-aop?id=${encodeURIComponent(id)}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers,
    })

    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }

    return json(keycloak, resp)
  } catch (e) {
    console.error('Error deleting release AOP:', e)
    return await Promise.reject(e)
  }
}

/**
 * Delete Release AOP by Plant ID and Year
 * Fetches the active release record for plant & year, then calls delete.
 */
async function deleteReleaseAOPByPlantAndYear(keycloak, plantId, year) {
  try {
    const statusResp = await getReleaseAOPStatus(keycloak, plantId, year)
    let releaseId = null

    if (Array.isArray(statusResp?.data) && statusResp.data.length > 0) {
      releaseId = statusResp.data[0].id || statusResp.data[0].Id
    } else if (statusResp?.data?.id || statusResp?.data?.Id) {
      releaseId = statusResp.data.id || statusResp.data.Id
    }

    if (releaseId) {
      return await deleteReleaseAOP(keycloak, releaseId)
    }
    return null
  } catch (e) {
    console.error('Error in deleteReleaseAOPByPlantAndYear:', e)
    return null
  }
}

export default ReleaseAPIService

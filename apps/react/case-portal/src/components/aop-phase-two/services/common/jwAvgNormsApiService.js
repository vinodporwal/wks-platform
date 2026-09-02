import Config from 'consts/index'
import { json } from 'services/request'
import { ImportExportApiService } from 'components/aop-phase-two/services/common/importExportApiService'

export const JWAvgNormsApiService = {
  getJobWorkAvgNormsData,
  saveJobWorkAvgNormsData,
  exportJobWorkAvgNormsExcel,
  importJobWorkAvgNormsExcel,
}

/**
 * Get Job Work Avg Norms data
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Job Work Avg Norms data
 */
async function getJobWorkAvgNormsData(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/job-work-avg-norms?plantId=${plantId}&aopYear=${year}`
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
    console.error('Error in getJobWorkAvgNormsData:', e)
    return await Promise.reject(e)
  }
}

/**
 * Save Job Work Avg Norms data
 * @param {Object} keycloak - Keycloak session
 * @param {Array} payload - Array of JobWorkAvgNormsDTO items
 * @returns {Promise} Save response
 */
async function saveJobWorkAvgNormsData(keycloak, payload) {
  const url = `${Config.CaseEngineUrl}/task/job-work-avg-norms`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error in saveJobWorkAvgNormsData:', e)
    return await Promise.reject(e)
  }
}

/**
 * Export Job Work Avg Norms Excel file
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} [fileName] - Download file name
 * @returns {Promise}
 */
async function exportJobWorkAvgNormsExcel(keycloak, plantId, year, fileName) {
  const queryParams = { plantId, aopYear: year }
  const queryString = new URLSearchParams(queryParams).toString()
  const url = `${Config.CaseEngineUrl}/task/job-work-avg-norms/export${queryString ? `?${queryString}` : ''}`

  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`Failed to export Excel: ${resp.status} ${resp.statusText}`)
    }

    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = fileName || `Job_Work_Avg_Norms_${year}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)

    return { success: true, message: 'Excel exported successfully' }
  } catch (e) {
    console.error('Error in exportJobWorkAvgNormsExcel:', e)
    return Promise.reject(e)
  }
}

/**
 * Import Job Work Avg Norms Excel file
 * @param {File} file - Uploaded Excel file
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise}
 */
async function importJobWorkAvgNormsExcel(file, keycloak, plantId, year) {
  const formData = new FormData()
  formData.append('file', file)

  const url = `${Config.CaseEngineUrl}/task/job-work-avg-norms/import?plantId=${encodeURIComponent(plantId)}&aopYear=${encodeURIComponent(year)}`
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }

  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error in importJobWorkAvgNormsExcel:', e)
    return await Promise.reject(e)
  }
}

export default JWAvgNormsApiService

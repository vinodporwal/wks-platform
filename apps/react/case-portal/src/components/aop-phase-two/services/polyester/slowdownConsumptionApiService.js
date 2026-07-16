import Config from 'consts/index'
import { json } from 'services/request'

export const SlowdownConsumptionApiService = {
  getSlowdownConsumption,
  saveSlowdownConsumption,
  exportSlowdownConsumption,
  importSlowdownConsumption,
  getGradesForSlowdownNorms,
  slowdownNormsExportAllGrade,
  saveSlowdownNormsExcel,
}

// ========================|| Slowdown Consumption APIs ||=====================================//

/**
 * Get Slowdown Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Slowdown consumption data
 */
async function getSlowdownConsumption(keycloak, plantId, year, gradeId) {
  let url
  if (gradeId) {
    url = `${Config.CaseEngineUrl}/task/slowdownNorms?year=${year}&plantId=${plantId}&gradeId=${gradeId}`
  } else {
    url = `${Config.CaseEngineUrl}/task/slowdownNorms?year=${year}&plantId=${plantId}`
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
 * Save Slowdown Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {Array} payload - Slowdown consumption data to save
 * @returns {Promise} Save response
 */
async function saveSlowdownConsumption(keycloak, plantId, year, payload) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-consumption?plantId=${plantId}`
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
 * Export Slowdown Consumption to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise<Blob>} Excel file blob
 */
async function exportSlowdownConsumption(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-consumption-export?plantId=${plantId}&aopYear=${year}`
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.statusText}`)
    }
    return await resp.blob()
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Import Slowdown Consumption from Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {File} file - Excel file to import
 * @returns {Promise} Import response
 */
async function importSlowdownConsumption(keycloak, plantId, year, file) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-consumption-import`
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
 * Get Grades for Slowdown Norms
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} List of grades
 */

async function getGradesForSlowdownNorms(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-norms-grades?plantId=${plantId}&year=${year}`
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
 * Export Slowdown Norms for All Grades
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} plantName - Plant name
 * @param {string} siteName - Site name
 * @param {string} verticalName - Vertical name
 * @param {boolean} allGrade - Export all grades flag
 * @returns {Promise<Blob>} Excel file blob
 */
async function slowdownNormsExportAllGrade(
  keycloak,
  plantId,
  year,
  plantName,
  siteName,
  verticalName,
  allGrade,
) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-norms-export-all-grade?plantId=${plantId}&year=${year}&plantName=${plantName}&siteName=${siteName}&verticalName=${verticalName}&allGrade=${allGrade}`
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.statusText}`)
    }
    return await resp.blob()
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Save Slowdown Norms from Excel
 * @param {File} file - Excel file to import
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} gradeId - Grade ID (optional)
 * @param {boolean} allGrade - All grades flag
 * @returns {Promise} Import response
 */
async function saveSlowdownNormsExcel(
  file,
  keycloak,
  plantId,
  year,
  gradeId,
  allGrade,
) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-norms-excel-import`
  const formData = new FormData()
  formData.append('file', file)
  formData.append('plantId', plantId)
  formData.append('year', year)
  if (gradeId) {
    formData.append('gradeId', gradeId)
  }
  formData.append('allGrade', allGrade)
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

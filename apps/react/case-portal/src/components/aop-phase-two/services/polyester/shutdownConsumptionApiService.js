import Config from 'consts/index'
import { json } from 'services/request'

export const ShutdownConsumptionApiService = {
  getShutdownConsumption,
  saveShutdownConsumption,
  exportShutdownConsumption,
  importShutdownConsumption,
  getGradesForShutdownNorms,
  shutdownNormsExportAllGrade,
  saveShutdownNormsExcel,
}

// ========================|| Shutdown Consumption APIs ||=====================================//

/**
 * Get Shutdown Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Shutdown consumption data
 */
async function getShutdownConsumption(keycloak, plantId, year, gradeId) {
  let url = ``
  if (gradeId) {
    url = `${Config.CaseEngineUrl}/task/shutdown-consumption?year=${year}&plantId=${plantId}&gradeId=${gradeId}`
  } else {
    url = `${Config.CaseEngineUrl}/task/shutdown-consumption?year=${year}&plantId=${plantId}`
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
 * Save Shutdown Consumption Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {Array} payload - Shutdown consumption data to save
 * @returns {Promise} Save response
 */
async function saveShutdownConsumption(keycloak, plantId, year, payload) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-consumption?plantId=${plantId}`
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
 * Export Shutdown Consumption to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} excelName - excel Name
 * @returns {Promise<Blob>} Excel file blob
 */
async function exportShutdownConsumption(
  keycloak,
  plantId,
  year,
  excelName = 'SHUTDOWN_CONSUMPTION',
) {
  const url = `${Config.CaseEngineUrl}/task/export-shutdown-consumption?plantId=${plantId}&year=${year}`
  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${excelName}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
    return
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Import Shutdown Consumption from Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {File} file - Excel file to import
 * @returns {Promise} Import response
 */
async function importShutdownConsumption(keycloak, plantId, year, file) {
  const url = `${Config.CaseEngineUrl}/task/import-shutdown-consumption`
  const formData = new FormData()
  formData.append('file', file)
  formData.append('plantId', plantId)
  formData.append('year', year)
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
 * Get Grades for Shutdown Norms
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} List of grades
 */
async function getGradesForShutdownNorms(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/unique/grades?plantId=${plantId}&year=${year}`
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
 * Export Shutdown Norms for All Grades (PE/PP)
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} plantName - Plant name
 * @param {string} siteName - Site name
 * @param {string} verticalName - Vertical name
 * @param {boolean} allGrade - Export all grades flag
 * @returns {Promise<Blob>} Excel file blob
 */
async function shutdownNormsExportAllGrade(
  keycloak,
  plantId,
  year,
  plantName,
  siteName,
  verticalName,
  allGrade,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-norms-export-all-grade?plantId=${plantId}&year=${year}&plantName=${plantName}&siteName=${siteName}&verticalName=${verticalName}&allGrade=${allGrade}`
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
 * Save Shutdown Norms from Excel
 * @param {File} file - Excel file to import
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} gradeId - Grade ID (optional)
 * @param {boolean} allGrade - All grades flag
 * @returns {Promise} Import response
 */
async function saveShutdownNormsExcel(
  file,
  keycloak,
  plantId,
  year,
  gradeId,
  allGrade,
) {
  const url = `${Config.CaseEngineUrl}/task/shutdown-norms-excel-import`
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

import Config from 'consts/index'
import { json } from 'services/request'

export const SlowdownPlanApiService = {
  getSlowdownActivities,
  saveSlowdownActivities,
  deleteSlowdownActivity,
  importSlowdownActivities,
  exportSlowdownActivities,
  getGrades,
}

// ========================|| Slowdown Plan APIs ||=====================================//

/**
 * Get Slowdown Activities data
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Slowdown activities data
 */
async function getSlowdownActivities(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/slowdown?plantId=${plantId}&maintenanceTypeName=Slowdown&year=${year}`
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
 * Save Slowdown Activities data
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {Array} payload - Slowdown activities data
 * @returns {Promise} Save response
 */
async function saveSlowdownActivities(keycloak, plantId, year, payload) {
  const url = `${Config.CaseEngineUrl}/task/slowdown/${plantId}`
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
 * Delete a Slowdown Activity
 * @param {Object} keycloak - Keycloak session
 * @param {string} id - Activity ID
 * @param {string} plantId - Plant ID
 * @returns {Promise} Delete response
 */
async function deleteSlowdownActivity(keycloak, id, plantId) {
  const url = `${Config.CaseEngineUrl}/task/slowdown/${id}/${plantId}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'DELETE', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await resp.text()
    return result || { success: true }
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Import Slowdown Activities from Excel
 * @param {File} file - Excel file
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Import response
 */
async function importSlowdownActivities(file, keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-import?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}&maintenanceTypeName=Slowdown`
  const formData = new FormData()
  formData.append('file', file)
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
 * Export Slowdown Activities to Excel
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} EXCEL_EXPORT_TITLE - Title for downloaded Excel file
 * @returns {Promise} Export response
 */
async function exportSlowdownActivities(keycloak, plantId, year, EXCEL_NAME) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-export?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}&maintenanceTypeName=Slowdown`
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
    a.download = EXCEL_NAME
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Get Grades / Products for Dropdown
 * @param {Object} keycloak - Keycloak session
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Products/grades dropdown data
 */
async function getGrades(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/products?year=${year}&plantId=${plantId}`
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

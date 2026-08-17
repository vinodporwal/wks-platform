import Config from 'consts/index'
import { json } from 'services/request'

export const BusinessDemandApiService = {
  getBusinessDemand,
  saveBusinessDemand,
  exportBusinessDemand,
  importBusinessDemand,
  deleteBusinessDemand,
  getProductionTarget,
}

// ========================|| Business Demand APIs ||=====================================//

/**
 * Get Business Demand Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Business demand data
 */
async function getBusinessDemand(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/business-demand?year=${year}&plantId=${plantId}`
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
 * Save Business Demand Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {Array} payload - Business demand data to save
 * @returns {Promise} Save response
 */
async function saveBusinessDemand(keycloak, plantId, year, payload) {
  const url = `${Config.CaseEngineUrl}/task/business-demand`
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
 * Export Business Demand to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} excelExportTitle - File title prefix
 * @param {string} screenName - Screen title suffix
 * @returns {Promise}
 */
async function exportBusinessDemand(
  keycloak,
  plantId,
  year,
  excelExportTitle,
  screenName,
) {
  const url = `${Config.CaseEngineUrl}/task/business-demand-export-total?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
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
    a.download = `${excelExportTitle}_${screenName}.xlsx`
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
 * Import Business Demand from Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {File} file - Excel file to import
 * @returns {Promise} Import response
 */
async function importBusinessDemand(keycloak, plantId, year, file) {
  const url = `${Config.CaseEngineUrl}/task/business-demand-import-total?plantId=${plantId}&year=${year}`
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
 * Delete Business Demand Record
 * @param {Object} keycloak - Keycloak session object
 * @param {string} id - Record ID to delete
 * @returns {Promise} Delete response
 */
async function deleteBusinessDemand(keycloak, id) {
  const url = `${Config.CaseEngineUrl}/task/business-demand/${id}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers,
    })
    if (!resp.ok) {
      throw new Error(
        `Failed to delete data: ${resp.status} ${resp.statusText}`,
      )
    }
    return await resp.text()
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Get Production Target Reference Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Production target reference data
 */
async function getProductionTarget(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/production-target?plantId=${plantId}&year=${year}`
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

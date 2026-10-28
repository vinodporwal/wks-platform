import Config from 'consts/index'
import { json } from 'services/request'

export const ProductionTargetApiService = {
  getDesignCapacity,
  saveDesignCapacity,
  getMaxAchievedCapacity,
  getProposedOperatingCapacity,
  saveProposedOperatingCapacity,
  exportProductionTarget,
  importProductionTarget,
}

// ========================|| Production Target APIs ||=====================================//

/**
 * Get Design Capacity Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Design capacity data
 */
async function getDesignCapacity(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/design-capacity?plantId=${plantId}&year=${year}`
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
 * Save Design Capacity Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {Array} data - Design capacity data to save
 * @returns {Promise} Save response
 */
async function saveDesignCapacity(keycloak, plantId, year, data) {
  const url = `${Config.CaseEngineUrl}/task/design-capacity?plantId=${plantId}&year=${year}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
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
 * Get Max Achieved Capacity Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Max achieved capacity data
 */
async function getMaxAchievedCapacity(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/max-achieved-capacity?plantId=${plantId}&year=${year}`
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
 * Get Proposed Operating Capacity Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Proposed operating capacity data
 */
async function getProposedOperatingCapacity(keycloak, plantId, year) {
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

/**
 * Save Proposed Operating Capacity Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {Array} data - Proposed operating capacity data to save
 * @returns {Promise} Save response
 */
async function saveProposedOperatingCapacity(keycloak, plantId, year, data) {
  const url = `${Config.CaseEngineUrl}/task/production-target?plantId=${plantId}&year=${year}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'PUT',
      headers,
      body: JSON.stringify(data),
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
 * Export Production Target Data to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} type - Type of data to export ('design', 'max', 'main', 'common')
 * @param {string} excelExportTitle - Filename prefix
 * @returns {Promise}
 */
async function exportProductionTarget(
  keycloak,
  plantId,
  year,
  type,
  excelExportTitle,
) {
  let url = ''
  let filenameSuffix = ''

  if (type === 'design') {
    url = `${Config.CaseEngineUrl}/task/production-target-export?year=${year}&plantId=${plantId}&gridName=design`
    filenameSuffix = 'Design Capacity.xlsx'
  } else if (type === 'max') {
    url = `${Config.CaseEngineUrl}/task/production-target-export?year=${year}&plantId=${plantId}`
    filenameSuffix = 'max_achieved_capacity.xlsx'
  } else if (type === 'main') {
    url = `${Config.CaseEngineUrl}/task/production-target-export?year=${year}&plantId=${plantId}`
    filenameSuffix = 'Proposed_Operating_Capacity.xlsx'
  } else {
    url = `${Config.CaseEngineUrl}/task/production-target-export-excel?year=${year}&plantId=${plantId}`
    filenameSuffix = 'Production_Target.xlsx'
  }

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
    a.download = `${excelExportTitle}_${filenameSuffix}`
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
 * Import Production Target Data from Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {File} file - Excel file to import
 * @param {string} type - Type of data to import ('design', 'main')
 * @returns {Promise} Import response
 */
async function importProductionTarget(keycloak, plantId, year, file, type) {
  let url = ''
  if (type === 'design') {
    url = `${Config.CaseEngineUrl}/task/design-capacity-import?plantId=${plantId}&year=${year}`
  } else {
    url = `${Config.CaseEngineUrl}/task/production-target-import?plantId=${plantId}&year=${year}`
  }

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

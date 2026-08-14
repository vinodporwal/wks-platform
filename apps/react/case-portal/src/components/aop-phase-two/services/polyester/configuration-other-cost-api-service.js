import Config from 'consts/index'
import { json } from 'services/request'

export const ConfigurationOtherCostApiService = {
  getConfigurationOtherCostData,
  saveConfigurationOtherCostData,
  handleCalculateConfigurationOtherCost,
  getConfigurationOtherCostExcel,
  saveConfigurationOtherCostExcelData,
}

/**
 * Get Configuration Other Cost Data
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} Configuration other cost data
 */

async function getConfigurationOtherCostData(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/configuration-other-cost?year=${year}&plantFKId=${plantId}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Save Configuration Other Cost Data
 * @param {string} plantId - Plant ID
 * @param {Object} payload - Payload for saving configuration other cost
 * @param {Object} keycloak - Keycloak session object
 * @param {string} year - AOP Year
 * @returns {Promise} API response
 */

async function saveConfigurationOtherCostData(
  plantId,
  payload,
  keycloak,
  year,
) {
  const url = `${Config.CaseEngineUrl}/task/configuration-other-cost?year=${year}&plantFKId=${plantId}`
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
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

/**
 * Handle Calculate Configuration Other Cost
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {Object} keycloak - Keycloak session object
 * @returns {Promise} API response
 */

async function handleCalculateConfigurationOtherCost(plantId, year, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/calculate-configuration-other-cost?plantId=${plantId}&aopYear=${year}`
  const headers = {
    Accept: 'application/json',
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
    const data = await resp.json() // Parse JSON response
    return data
  } catch (e) {
    console.error('Error fetching calculation data:', e)
    return Promise.reject(e)
  }
}

/**
 * Get Configuration Other Cost Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @param {string} excelExportTitle - Excel export title
 * @param {string} screenName - Screen name
 * @returns {Promise} API response
 */

async function getConfigurationOtherCostExcel(
  keycloak,
  plantId,
  year,
  excelExportTitle,
  screenName,
) {
  var url = `${Config.CaseEngineUrl}/task/configuration-other-cost-export?year=${year}&plantFKId=${plantId}`

  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Failed to edit data: ${resp.status} ${resp.statusText}`)
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
    console.error('Error Editing data:', e)
    return Promise.reject(e)
  }
}

/**
 * Save Configuration Other Cost Excel Data
 * @param {Object} file - Excel file to upload
 * @param {Object} keycloak - Keycloak session object
 * @param {string} plantId - Plant ID
 * @param {string} year - AOP Year
 * @returns {Promise} API response
 */

async function saveConfigurationOtherCostExcelData(
  file,
  keycloak,
  plantId,
  year,
) {
  let url = ''
  url = `${Config.CaseEngineUrl}/task/configuration-other-cost-import?plantFKId=${plantId}&year=${year}`

  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
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
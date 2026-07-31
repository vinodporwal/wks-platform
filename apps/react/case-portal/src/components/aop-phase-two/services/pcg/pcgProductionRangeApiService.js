import Config from 'consts/index'
import { json } from 'services/request'

export const PCGProductionRangeApiService = {
  getData,
  postData,
  getDataForLimit,
  getProductionRangeExcel,
  getProductionRangeLimitExcel,
  productionRangeImport,
  productionRangeLimitImport,
}

// ========================|| PCG Production Range APIs ||=====================================//

/**
 * Fetch all production range records for a PCG plant
 * GET /task/production-range?year=&plantId=
 *
 * @param {Object} keycloak   - Keycloak session
 * @param {string} plantId    - Plant UUID
 * @param {string} year       - AOP year (e.g. "2026-27")
 * @returns {Promise<Object>} - Response containing productionRangeList
 */
async function getData(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/production-range?year=${AOP_YEAR}&plantId=${PLANT_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching PCG Production Range data:', e)
    return await Promise.reject(e)
  }
}

/**
 * Fetch production range LIMIT records for a PCG plant
 * GET /task/production-range-limit?year=&plantId=
 *
 * @param {Object} keycloak   - Keycloak session
 * @param {string} plantId    - Plant UUID
 * @param {string} year       - AOP year (e.g. "2026-27")
 * @returns {Promise<Object>} - Response containing productionRangeLimitList
 */
async function getDataForLimit(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/production-range-limit?year=${AOP_YEAR}&plantId=${PLANT_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching PCG Production Range Limit data:', e)
    return await Promise.reject(e)
  }
}

/**
 * Save production range / limit data for a PCG plant
 * POST /task/production-norms?year=&plantFKId=
 *
 * @param {Object} keycloak   - Keycloak session
 * @param {Array}  payload    - Array of production range records to save
 * @param {string} PLANT_ID   - Plant UUID
 * @param {string} AOP_YEAR   - AOP year (e.g. "2026-27")
 * @returns {Promise<Object>} - Response from API
 */
async function postData(keycloak, payload, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/production-norms?year=${AOP_YEAR}&plantFKId=${PLANT_ID}`
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
      throw new Error(`Failed to save data: ${resp.status} ${resp.statusText}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error saving PCG production norms data:', e)
    return Promise.reject(e)
  }
}

/**
 * Export Production Range data to Excel for a PCG plant
 * GET /task/production-range-export?year=&plantId=
 *
 * @param {Object} keycloak          - Keycloak session
 * @param {string} PLANT_ID          - Plant UUID
 * @param {string} AOP_YEAR          - AOP year (e.g. "2026-27")
 * @param {string} EXCEL_EXPORT_TITLE - File name prefix
 */
async function getProductionRangeExcel(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  EXCEL_EXPORT_TITLE,
) {
  const url = `${Config.CaseEngineUrl}/task/production-range-export?year=${AOP_YEAR}&plantId=${PLANT_ID}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(
        `Failed to export data: ${resp.status} ${resp.statusText}`,
      )
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Production_Range.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting PCG Production Range Excel:', e)
    return Promise.reject(e)
  }
}

/**
 * Export Production Range Limit data to Excel for a PCG plant
 * GET /task/production-range-limit-export?year=&plantId=
 *
 * @param {Object} keycloak          - Keycloak session
 * @param {string} PLANT_ID          - Plant UUID
 * @param {string} AOP_YEAR          - AOP year (e.g. "2026-27")
 * @param {string} EXCEL_EXPORT_TITLE - File name prefix
 */
async function getProductionRangeLimitExcel(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  EXCEL_EXPORT_TITLE,
) {
  const url = `${Config.CaseEngineUrl}/task/production-range-limit-export?year=${AOP_YEAR}&plantId=${PLANT_ID}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(
        `Failed to export data: ${resp.status} ${resp.statusText}`,
      )
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_Production_Range_Limit.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting PCG Production Range Limit Excel:', e)
    return Promise.reject(e)
  }
}

/**
 * Import production range data from Excel for a PCG plant
 * POST /task/production-range-import?plantId=&year=&isMinMax=true
 *
 * @param {File}   file       - Excel file containing production range data
 * @param {Object} keycloak   - Keycloak session
 * @param {string} PLANT_ID   - Plant UUID
 * @param {string} AOP_YEAR   - AOP year (e.g. "2026-27")
 * @returns {Promise<Object>} - Response from API
 */
async function productionRangeImport(file, keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/production-range-import?plantId=${PLANT_ID}&year=${AOP_YEAR}&isMinMax=true`
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
    console.error('Error importing PCG Production Range Excel:', e)
    return await Promise.reject(e)
  }
}

/**
 * Import production range limit data from Excel for a PCG plant
 * POST /task/production-range-limit-import?plantId=&year=
 *
 * @param {File}   file       - Excel file containing production range limit data
 * @param {Object} keycloak   - Keycloak session
 * @param {string} PLANT_ID   - Plant UUID
 * @param {string} AOP_YEAR   - AOP year (e.g. "2026-27")
 * @returns {Promise<Object>} - Response from API
 */
async function productionRangeLimitImport(file, keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/production-range-limit-import?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
    console.error('Error importing PCG Production Range Limit Excel:', e)
    return await Promise.reject(e)
  }
}

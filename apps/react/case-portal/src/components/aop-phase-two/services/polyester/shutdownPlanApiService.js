import Config from 'consts/index'
import { json } from 'services/request'

export const ShutdownPlanApiService = {
  getShutdownPlan,
  saveShutdownPlan,
  deleteShutdownActivity,
  exportShutdownPlan,
  importShutdownPlan,
  deleteMultipleShutdown,
  exportShutdownNonProduct,
  importShutdownNonProduct,
  getShutdownPlanWithValue,
  exportShutdownPlanWithValue,
  dropdownValuesShutdownDesc,
  importShutdownPlanForNonProduct,
}

// ========================|| PE Shutdown Plan APIs ||=====================================//

/**
 * Fetch all shutdown activities for a PE plant
 * GET /task/shutdown?plantId=&maintenanceTypeName=Shutdown&year=
 *
 * @param {Object} keycloak   - Keycloak session
 * @param {string} plantId    - Plant UUID
 * @param {string} year       - AOP year (e.g. "2026-27")
 * @returns {Promise<Array>}  - Plain array of shutdown records
 */
async function getShutdownPlan(keycloak, plantId, year) {
  const url =
    `${Config.CaseEngineUrl}/task/shutdown` +
    `?plantId=${encodeURIComponent(plantId)}` +
    `&maintenanceTypeName=Shutdown` +
    `&year=${encodeURIComponent(year)}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) throw new Error(`HTTP error! Status: ${resp.status}`)
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching shutdown plan data:', e)
    return Promise.reject(e)
  }
}

/**
 * Save (create / update) shutdown activities for a PE plant
 * POST /task/shutdown/:plantId
 *
 * Payload shape per record:
 * {
 *   id:                 string | null,   // null for new records
 *   productId:          null,            // not used for PE NMD non-product
 *   productName:        null,
 *   discription:        string,
 *   durationInHrs:      string,          // "HH.MM" e.g. "10.30"
 *   maintStartDateTime: Date | null,
 *   maintEndDateTime:   Date | null,
 *   audityear:          string,
 *   remark:             string,
 *   shutdownRate:       null,
 * }
 *
 * @param {Object} keycloak         - Keycloak session
 * @param {string} plantId          - Plant UUID
 * @param {Array}  shutdownDetails  - Array of payload objects
 * @returns {Promise}
 */
async function saveShutdownPlan(keycloak, plantId, shutdownDetails) {
  const url = `${Config.CaseEngineUrl}/task/shutdown/${encodeURIComponent(plantId)}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(shutdownDetails),
    })
    if (!resp.ok) throw new Error(`HTTP error! Status: ${resp.status}`)
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error saving shutdown plan data:', e)
    return Promise.reject(e)
  }
}

/**
 * Delete a single shutdown activity
 * DELETE /task/shutdown/:maintenanceId/:plantId
 *
 * @param {Object} keycloak       - Keycloak session
 * @param {string} maintenanceId  - The idFromApi of the record to delete
 * @param {string} plantId        - Plant UUID
 * @returns {Promise<string>}     - Plain text response
 */
async function deleteShutdownActivity(keycloak, maintenanceId, plantId) {
  const url =
    `${Config.CaseEngineUrl}/task/shutdown` +
    `/${encodeURIComponent(maintenanceId)}` +
    `/${encodeURIComponent(plantId)}`

  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'DELETE', headers })
    if (!resp.ok) throw new Error(`HTTP error! Status: ${resp.status}`)
    return resp.text()
  } catch (e) {
    console.error('Error deleting shutdown activity:', e)
    return Promise.reject(e)
  }
}

/**
 * Export all shutdown activities for a PE plant as Excel
 * GET /task/shutdown-export-non-product?year=&plantId=&maintenanceTypeName=Shutdown
 *
 * Downloads file directly to browser.
 *
 * @param {Object} keycloak      - Keycloak session
 * @param {string} plantId       - Plant UUID
 * @param {string} year          - AOP year
 * @param {string} excelTitle    - Filename prefix (e.g. "PE_NMD_LLDPE1")
 * @returns {Promise<void>}
 */
async function exportShutdownPlan(keycloak, plantId, year, excelTitle) {
  const url =
    `${Config.CaseEngineUrl}/task/shutdown-export-hiir` +
    `?year=${encodeURIComponent(year)}` +
    `&plantId=${encodeURIComponent(plantId)}` +
    `&maintenanceTypeName=Shutdown`

  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) throw new Error(`Export failed: ${resp.status}`)

    const blob = await resp.blob()
    const blobUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = `${excelTitle}_Shutdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(blobUrl)
  } catch (e) {
    console.error('Error exporting shutdown plan:', e)
    return Promise.reject(e)
  }
}

/**
 * Import shutdown activities for a PE plant from Excel
 * POST /task/shutdown-import-non-product?plantId=&year=&maintenanceTypeName=Shutdown
 *
 * Response shape:
 *   { code: 200, message, data }        → success
 *   { code: 400, message, data: base64} → partial save; data = error excel (base64)
 *
 * @param {File}   file      - Excel file to upload
 * @param {Object} keycloak  - Keycloak session
 * @param {string} plantId   - Plant UUID
 * @param {string} year      - AOP year
 * @returns {Promise<{code, message, data}>}
 */
async function importShutdownPlan(file, keycloak, plantId, year) {
  const url =
    `${Config.CaseEngineUrl}/task/shutdown-import-hiir` +
    `?plantId=${encodeURIComponent(plantId)}` +
    `&year=${encodeURIComponent(year)}` +
    `&maintenanceTypeName=Shutdown`

  const formData = new FormData()
  formData.append('file', file)

  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: formData })
    return resp.json()
  } catch (e) {
    console.error('Error importing shutdown plan:', e)
    return Promise.reject(e)
  }
}

// ─── Delete Selected ───────────────────────────────────────────────────────────────────
/**
 * Delete multiple shutdown activities for a PE plant
 * DELETE /task/shutdown?plantMaintenanceTransactionIds={id1,id2,id3...}&plantId={plantId}
 *
 * Response shape:
 *   { message: "Success" }        → success
 *
 * @param {Array}   ids  - Array of IDs to delete
 * @param {Object} keycloak  - Keycloak session
 * @param {string} PLANT_ID   - Plant ID
 * @returns {Promise<string>}
 */

/**
 * Export all shutdown activities for a plant as Excel (non-product)
 * GET /task/shutdown-export-non-product?year=&plantId=&maintenanceTypeName=Shutdown
 *
 * @param {Object} keycloak      - Keycloak session
 * @param {string} plantId       - Plant UUID
 * @param {string} year          - AOP year
 * @param {string} excelTitle    - Filename prefix
 * @returns {Promise<void>}
 */
async function exportShutdownNonProduct(keycloak, plantId, year, excelTitle) {
  const url =
    `${Config.CaseEngineUrl}/task/shutdown-export-non-product` +
    `?year=${encodeURIComponent(year)}` +
    `&plantId=${encodeURIComponent(plantId)}` +
    `&maintenanceTypeName=Shutdown`

  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) throw new Error(`Export failed: ${resp.status}`)

    const blob = await resp.blob()
    const blobUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = `${excelTitle}_Shutdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(blobUrl)
  } catch (e) {
    console.error('Error exporting shutdown non-product:', e)
    return Promise.reject(e)
  }
}

/**
 * Import shutdown activities for a plant from Excel (non-product)
 * POST /task/shutdown-import-non-product?plantId=&year=&maintenanceTypeName=Shutdown
 *
 * @param {File}   file      - Excel file to upload
 * @param {Object} keycloak  - Keycloak session
 * @param {string} plantId   - Plant UUID
 * @param {string} year      - AOP year
 * @returns {Promise<{code, message, data}>}
 */
async function importShutdownNonProduct(file, keycloak, plantId, year) {
  const url =
    `${Config.CaseEngineUrl}/task/shutdown-import-non-product` +
    `?plantId=${encodeURIComponent(plantId)}` +
    `&year=${encodeURIComponent(year)}` +
    `&maintenanceTypeName=Shutdown`

  const formData = new FormData()
  formData.append('file', file)

  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: formData })
    return resp.json()
  } catch (e) {
    console.error('Error importing shutdown non-product:', e)
    return Promise.reject(e)
  }
}

async function deleteMultipleShutdown(ids, keycloak, PLANT_ID) {
  const url = `${Config.CaseEngineUrl}/task/shutdown?plantMaintenanceTransactionIds=${ids.join(',')}&plantId=${PLANT_ID}`
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
    return json(keycloak, resp) // Handle text response from the backend
  } catch (e) {
    console.error('Error deleting multiple shutdown data:', e)
    return Promise.reject(e)
  }
}
async function getShutdownPlanWithValue(keycloak, plantId, year) {
  const url =
    `${Config.CaseEngineUrl}/task/shutdown-with-value` +
    `?plantId=${encodeURIComponent(plantId)}` +
    `&maintenanceTypeName=Shutdown` +
    `&year=${encodeURIComponent(year)}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) throw new Error(`HTTP error! Status: ${resp.status}`)
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching shutdown plan data:', e)
    return Promise.reject(e)
  }
}
async function exportShutdownPlanWithValue(keycloak, plantId, year, excelTitle) {
  const url =
    `${Config.CaseEngineUrl}/task/shutdown-export-non-product` +
    `?year=${encodeURIComponent(year)}` +
    `&plantId=${encodeURIComponent(plantId)}` +
    `&maintenanceTypeName=Shutdown`

  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) throw new Error(`Export failed: ${resp.status}`)

    const blob = await resp.blob()
    const blobUrl = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = `${excelTitle}_Shutdown Activities.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(blobUrl)
  } catch (e) {
    console.error('Error exporting shutdown non-product:', e)
    return Promise.reject(e)
  }
}
async function dropdownValuesShutdownDesc(keycloak, PLANT_ID, AOP_YEAR) {
  // const url = `${Config.CaseEngineUrl}/task/description-drpdwn?plantId=${PLANT_ID}&year=${AOP_YEAR}`
  const url = `${Config.CaseEngineUrl}/task/shutdown-description?plantId=${PLANT_ID}`
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
async function importShutdownPlanForNonProduct(file, keycloak, plantId, year) {
  const url =
    `${Config.CaseEngineUrl}/task/shutdown-import-non-product` +
    `?plantId=${encodeURIComponent(plantId)}` +
    `&year=${encodeURIComponent(year)}` +
    `&maintenanceTypeName=Shutdown`

  const formData = new FormData()
  formData.append('file', file)

  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: formData })
    return resp.json()
  } catch (e) {
    console.error('Error importing shutdown plan:', e)
    return Promise.reject(e)
  }
}
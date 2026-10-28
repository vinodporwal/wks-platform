import Config from 'consts/index'
import { json } from 'services/request'

export const ShutdownActivitiesApiService = {
  getShutdownActivities,
  saveShutdownActivities,
  deleteShutdownActivity,
  exportShutdownActivities,
  importShutdownActivities,
  deleteMultipleShutdown,
}

// ========================|| Shutdown Activities APIs ||=====================================//

/**
 * Fetch all shutdown activities for a plant
 * GET /task/shutdown?plantId=&maintenanceTypeName=Shutdown&year=
 *
 * @param {Object} keycloak   - Keycloak session
 * @param {string} plantId    - Plant UUID
 * @param {string} year       - AOP year (e.g. "2026-27")
 * @returns {Promise<Array>}  - Plain array of shutdown records
 */
async function getShutdownActivities(keycloak, plantId, year) {
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
    console.error('Error fetching shutdown activities:', e)
    return Promise.reject(e)
  }
}

/**
 * Save (create / update) shutdown activities for a plant
 * POST /task/shutdown/:plantId
 *
 * @param {Object} keycloak         - Keycloak session
 * @param {string} plantId          - Plant UUID
 * @param {Array}  shutdownDetails  - Array of payload objects
 * @returns {Promise}
 */
async function saveShutdownActivities(keycloak, plantId, shutdownDetails) {
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
    console.error('Error saving shutdown activities:', e)
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
 * Export all shutdown activities for a plant as Excel
 * GET /task/shutdown-export-hiir?year=&plantId=&maintenanceTypeName=Shutdown
 *
 * Downloads file directly to browser.
 *
 * @param {Object} keycloak      - Keycloak session
 * @param {string} plantId       - Plant UUID
 * @param {string} year          - AOP year
 * @param {string} excelTitle    - Filename prefix (e.g. "Shutdown_Activities")
 * @returns {Promise<void>}
 */
async function exportShutdownActivities(keycloak, plantId, year, excelTitle) {
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
    console.error('Error exporting shutdown activities:', e)
    return Promise.reject(e)
  }
}

/**
 * Import shutdown activities for a plant from Excel
 * POST /task/shutdown-import-hiir?plantId=&year=&maintenanceTypeName=Shutdown
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
async function importShutdownActivities(file, keycloak, plantId, year) {
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
    console.error('Error importing shutdown activities:', e)
    return Promise.reject(e)
  }
}

/**
 * Delete multiple shutdown activities for a plant
 * DELETE /task/shutdown?plantMaintenanceTransactionIds={id1,id2,id3...}&plantId={plantId}
 *
 * @param {Array}   ids       - Array of IDs to delete
 * @param {Object}  keycloak  - Keycloak session
 * @param {string}  plantId   - Plant ID
 * @returns {Promise<string>}
 */
async function deleteMultipleShutdown(ids, keycloak, plantId) {
  const url = `${Config.CaseEngineUrl}/task/shutdown?plantMaintenanceTransactionIds=${ids.join(',')}&plantId=${plantId}`
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
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error deleting multiple shutdown data:', e)
    return Promise.reject(e)
  }
}

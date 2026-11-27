import Config from 'consts/index'
import { json } from 'services/request'

export const PCGShutdownTaApiService = {
  getShutdownTaTransactions,
  getShutdownTaTransactionsSng,
  getShutdownTaGasifierDropdown,
  saveShutdownTaTransactions,
  saveShutdownTaTransactionsSng,
  getAopProductionNorms,
  exportShutdownPlan,
  exportShutdownPlanSng,
  importShutdownPlan,
  importShutdownPlanSng,
}

/**
 * Get Gasifier Dropdown
 * GET /gasifier-dropdown?plantId=&aopYear=
 */
async function getShutdownTaGasifierDropdown(keycloak, plantId, aopYear) {
  let url = `${Config.CaseEngineUrl}/task/gasifier-dropdown`
  const params = []
  if (plantId) params.push(`plantId=${encodeURIComponent(plantId)}`)
  if (aopYear) params.push(`aopYear=${encodeURIComponent(aopYear)}`)
  if (params.length > 0) url += `?${params.join('&')}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching gasifier dropdown:', e)
    return Promise.reject(e)
  }
}

/**
 * Get Shutdown/TA Transactions
 * GET /shutdown-transactions?plantId=&aopYear=
 */
async function fetchShutdownTaTransactions(
  keycloak,
  plantId,
  aopYear,
  endpointPath,
) {
  let url = `${Config.CaseEngineUrl}${endpointPath}`
  const params = []
  if (plantId) params.push(`plantId=${encodeURIComponent(plantId)}`)
  if (aopYear) params.push(`aopYear=${encodeURIComponent(aopYear)}`)
  if (params?.length > 0) url += `?${params?.join('&')}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching shutdown transactions:', e)
    return Promise.reject(e)
  }
}

async function getShutdownTaTransactions(keycloak, plantId, aopYear) {
  return fetchShutdownTaTransactions(
    keycloak,
    plantId,
    aopYear,
    '/task/shutdown-transactions',
  )
}

/**
 * Get Shutdown/TA Transactions for SNG
 * GET /task/shutdown-transactions-sng?plantId=&aopYear=
 */
async function getShutdownTaTransactionsSng(keycloak, plantId, aopYear) {
  return fetchShutdownTaTransactions(
    keycloak,
    plantId,
    aopYear,
    '/task/shutdown-transactions-sng',
  )
}

/**
 * Save Shutdown/TA Transactions
 * POST /shutdown-transactions?plantId=&aopYear=
 * Body: List<ShutdownTaTransactionDTO>
 */
async function postShutdownTaTransactions(
  keycloak,
  plantId,
  payload,
  aopYear,
  endpointPath,
) {
  let url = `${Config.CaseEngineUrl}${endpointPath}`
  const params = []
  if (plantId) params.push(`plantId=${encodeURIComponent(plantId)}`)
  if (aopYear) params.push(`aopYear=${encodeURIComponent(aopYear)}`)
  if (params?.length > 0) url += `?${params?.join('&')}`

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
    console.error('Error saving shutdown transactions:', e)
    return Promise.reject(e)
  }
}

async function saveShutdownTaTransactions(keycloak, plantId, payload, aopYear) {
  return postShutdownTaTransactions(
    keycloak,
    plantId,
    payload,
    aopYear,
    '/task/shutdown-transactions',
  )
}

/**
 * Save Shutdown/TA Transactions for SNG
 * POST /task/shutdown-transactions-sng?plantId=&aopYear=
 */
async function saveShutdownTaTransactionsSng(
  keycloak,
  plantId,
  payload,
  aopYear,
) {
  return postShutdownTaTransactions(
    keycloak,
    plantId,
    payload,
    aopYear,
    '/task/shutdown-transactions-sng',
  )
}

/**
 * Get AOP Production Norms
 * GET /task/aop-production-norms?plantId=&aopYear=
 */
async function getAopProductionNorms(keycloak, plantId, aopYear) {
  let url = `${Config.CaseEngineUrl}/task/aop-production-norms`
  const params = []
  if (plantId) params.push(`plantId=${encodeURIComponent(plantId)}`)
  if (aopYear) params.push(`aopYear=${encodeURIComponent(aopYear)}`)
  if (params.length > 0) url += `?${params.join('&')}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching AOP production norms:', e)
    return Promise.reject(e)
  }
}

/**
 * Export all shutdown activities for a PE plant as Excel
 * GET /task/shutdown-transactions-export?plantId=&aopYear=
 *
 * Downloads file directly to browser.
 *
 * @param {Object} keycloak      - Keycloak session
 * @param {string} plantId       - Plant UUID
 * @param {string} year          - AOP year
 * @param {string} excelTitle    - Filename prefix (e.g. "PE_NMD_LLDPE1")
 * @returns {Promise<void>}
 */
async function exportShutdownTaFile(
  keycloak,
  plantId,
  aopYear,
  excelTitle,
  endpointPath,
) {
  const url =
    `${Config.CaseEngineUrl}${endpointPath}` +
    `?aopYear=${encodeURIComponent(aopYear)}` +
    `&plantId=${encodeURIComponent(plantId)}`

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

async function exportShutdownPlan(keycloak, plantId, aopYear, excelTitle) {
  return exportShutdownTaFile(
    keycloak,
    plantId,
    aopYear,
    excelTitle,
    '/task/shutdown-transactions-export',
  )
}

/**
 * Export shutdown activities for PCG SNG plant as Excel
 * GET /task/shutdown-transactions-export-sng?plantId=&aopYear=
 */
async function exportShutdownPlanSng(keycloak, plantId, aopYear, excelTitle) {
  return exportShutdownTaFile(
    keycloak,
    plantId,
    aopYear,
    excelTitle,
    '/task/shutdown-transactions-export-sng',
  )
}

/**
 * Import shutdown activities for a PE plant from Excel
 * POST /task/shutdown-transactions-import?plantId=&aopYear=
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
async function importShutdownTaFile(
  file,
  keycloak,
  plantId,
  aopYear,
  endpointPath,
) {
  const url =
    `${Config.CaseEngineUrl}${endpointPath}` +
    `?plantId=${encodeURIComponent(plantId)}` +
    `&aopYear=${encodeURIComponent(aopYear)}`

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

async function importShutdownPlan(file, keycloak, plantId, aopYear) {
  return importShutdownTaFile(
    file,
    keycloak,
    plantId,
    aopYear,
    '/task/shutdown-transactions-import',
  )
}

/**
 * Import shutdown activities for PCG SNG plant from Excel
 * POST /task/shutdown-transactions-import-sng?plantId=&aopYear=
 */
async function importShutdownPlanSng(file, keycloak, plantId, aopYear) {
  return importShutdownTaFile(
    file,
    keycloak,
    plantId,
    aopYear,
    '/task/shutdown-transactions-import-sng',
  )
}

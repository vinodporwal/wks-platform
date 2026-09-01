import Config from 'consts/index'
import { json } from 'services/request'

export const SteadyStateConsumptionApiService = {
  // PE-specific (grade-based) — same endpoints as NormalOperationNormsApiService
  getSteadyStateConsumptionByGrade,
  validateGradeSteadyStateNorms,
  getGrades,
  getNormTransactions,
  saveSteadyStateConsumptionByGrade,
  calculateSteadyStateConsumptionPE,
  exportSteadyStateConsumptionPE,
  importSteadyStateConsumptionByGrade,
  saveGradeWiseSteadyStateConsumption,
  // Generic (kept for backward compat)
  getSteadyStateConsumption,
  saveSteadyStateConsumption,
  exportSteadyStateConsumption,
  importSteadyStateConsumption,
  calculateSteadyStateConsumption,
  // filament
  calculateSteadyStateConsumptionPolyester,
}

// ========================|| Steady State Consumption - PE/NMD Specific (same endpoints as NormalOperationNormsApiService) ||===//

/**
 * Validate Grade Steady State Norms
 * Endpoint: GET /task/steady-state-norms/grade/validation?plantId=&year=&gradeId=
 */
async function validateGradeSteadyStateNorms(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  gradeId,
) {
  const queryParams = new URLSearchParams({
    plantId: PLANT_ID,
    year: AOP_YEAR,
  })
  if (gradeId) queryParams.append('gradeId', gradeId)
  const url = `${Config.CaseEngineUrl}/task/steady-state-norms/grade/validation?${queryParams.toString()}`
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
 * Get Steady State Consumption by Grade (for PE/PP vertical)
 * Same as NormalOperationNormsApiService.getNormalOperationNormsData(keycloak, gradeId, false, PLANT_ID, AOP_YEAR)
 * Endpoint: /task/steady-state-norms?year=&plantId=&gradeId=
 */
async function getSteadyStateConsumptionByGrade(
  keycloak,
  gradeId,
  PLANT_ID,
  AOP_YEAR,
) {
  const queryParams = new URLSearchParams({ year: AOP_YEAR, plantId: PLANT_ID })
  if (gradeId) queryParams.append('gradeId', gradeId)
  const url = `${Config.CaseEngineUrl}/task/steady-state-norms?${queryParams.toString()}`
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
 * Get Grade Dropdown for PE/PP vertical
 * Same as NormalOperationNormsApiService.getNormalOperationNormsGrades
 * Endpoint: /task/normal-operation/norms/grades?year=&plantId=
 */
async function getGrades(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/normal-operation/norms/grades?year=${AOP_YEAR}&plantId=${PLANT_ID}`
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
 * Get Norm Transactions (allRedCell) — for red cell highlighting
 * Same as NormalOperationNormsApiService.getNormTransactions
 * Endpoint: /task/norms-transactions?plantId=&year=
 */
async function getNormTransactions(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/norms-transactions?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
 * Save Steady State Consumption by Grade (PE/PP vertical)
 * Same as NormalOperationNormsApiService.saveNormalOperationNormsData(..., 'pe', ...)
 * Endpoint: POST /task/steady-state-norms?year=&plantId=&gradeId=
 */
async function saveSteadyStateConsumptionByGrade(
  PLANT_ID,
  payload,
  keycloak,
  gradeId,
  AOP_YEAR,
) {
  const queryParams = new URLSearchParams({ year: AOP_YEAR, plantId: PLANT_ID })
  if (gradeId) queryParams.append('gradeId', gradeId)
  const url = `${Config.CaseEngineUrl}/task/steady-state-norms?${queryParams.toString()}`
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
 * Calculate Steady State Consumption for PE vertical (uses plant + site + vertical)
 * Same as NormalOperationNormsApiService.handleCalculateNormalOperationNormsPe
 * Endpoint: GET /task/calculate-normal-ops-norms?plantId=&siteId=&verticalId=&aopYear=
 */
async function calculateSteadyStateConsumptionPE(
  plantId,
  siteId,
  verticalId,
  year,
  keycloak,
) {
  const url = `${Config.CaseEngineUrl}/task/calculate-normal-ops-norms?plantId=${plantId}&siteId=${siteId}&verticalId=${verticalId}&aopYear=${year}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) throw new Error(`HTTP error! Status: ${resp.status}`)
    return await resp.json()
  } catch (e) {
    console.error('Error calculating steady state norms (PE):', e)
    return Promise.reject(e)
  }
}

/**
 * Calculate Steady State Consumption for Polyester vertical (uses plant + site + vertical)
 * Same as NormalOperationNormsApiService.handleCalculateNormalOperationNormsPe
 * Endpoint: GET /task/calculate-normal-ops-norms?plantId=&siteId=&verticalId=&aopYear=
 */
async function calculateSteadyStateConsumptionPolyester(
  plantId,
  siteId,
  verticalId,
  year,
  keycloak,
) {
  const url = `${Config.CaseEngineUrl}/task/calculate-normal-ops-norms/polyester?plantId=${plantId}&siteId=${siteId}&verticalId=${verticalId}&aopYear=${year}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) throw new Error(`HTTP error! Status: ${resp.status}`)
    return await resp.json()
  } catch (e) {
    console.error('Error calculating steady state norms (PE):', e)
    return Promise.reject(e)
  }
}

/**
 * Export Steady State Consumption for PE vertical (all grades)
 * Same as NormalOperationNormsApiService.getNormalOpsNormsExcelpe
 * Endpoint: GET /task/steady-state-norms-all-grades-export?year=&plantId=
 */
async function exportSteadyStateConsumptionPE(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  EXCEL_NAME,
) {
  const url = `${Config.CaseEngineUrl}/task/steady-state-norms-all-grades-export?year=${AOP_YEAR}&plantId=${PLANT_ID}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok)
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_NAME}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting steady state norms (PE all grades):', e)
    return Promise.reject(e)
  }
}

/**
 * Import Steady State Consumption by Grade (PE/PP vertical)
 * Same as NormalOperationNormsApiService.saveNormalOpsNormsExcel with gradeId
 * Endpoint: POST /task/steady-state-norms-import?plantId=&year=&gradeId=
 */
async function importSteadyStateConsumptionByGrade(
  file,
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  gradeId,
) {
  let url = `${Config.CaseEngineUrl}/task/steady-state-norms-import/polyester?plantId=${PLANT_ID}&year=${AOP_YEAR}`
  if (gradeId) url += `&gradeId=${gradeId}`
  const formData = new FormData()
  formData.append('file', file)
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'POST', headers, body: formData })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

// ========================|| Generic / Staple endpoints (kept for backward compat) ||=====================================//

async function getSteadyStateConsumption(keycloak, plantId, year) {
  const baseUrl = `${Config.CaseEngineUrl}/task/staple/steady-state-norms`
  const queryParams = new URLSearchParams({ year, plantId })
  const url = `${baseUrl}?${queryParams.toString()}`
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
    console.log(e)
    return await Promise.reject(e)
  }
}

async function saveSteadyStateConsumption(keycloak, plantId, year, data) {
  const baseUrl = `${Config.CaseEngineUrl}/task/staple/steady-state-norms`
  const queryParams = new URLSearchParams({ year, plantId })
  const url = `${baseUrl}?${queryParams.toString()}`
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
      const errorText = await resp.text()
      throw new Error(`HTTP ${resp.status}: ${errorText || resp.statusText}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error in saveSteadyStateConsumption:', e)
    return await Promise.reject(e)
  }
}

/**
 * Export Steady State Consumption to Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {Number} plantId - Plant ID
 * @param {Number} year - AOP Year
 * @param {String} EXCEL_NAME - Optional mode parameter
 * @param {String} mode - Optional mode parameter
 * @param {String} gradeId - Optional grade ID parameter
 * @returns {Promise<Blob>} Excel file blob
 */
async function exportSteadyStateConsumption(
  keycloak,
  plantId,
  year,
  EXCEL_NAME,
  mode,
  gradeId,
) {
  const baseUrl = `${Config.CaseEngineUrl}/task/steady-state-norms-export`
  const queryParams = new URLSearchParams({
    plantId,
    year,
  })

  if (mode) {
    queryParams.append('mode', mode)
  }
  if (gradeId) {
    queryParams.append('gradeId', gradeId)
  }

  const url = `${baseUrl}?${queryParams.toString()}`
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
    a.download = `${EXCEL_NAME}.xlsx`
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
 * Import Steady State Consumption from Excel
 * @param {Object} keycloak - Keycloak session object
 * @param {Number} plantId - Plant ID
 * @param {Number} year - AOP Year
 * @param {File} file - Excel file to import
 * @param {String} mode - Optional mode parameter
 * @param {String} gradeId - Optional grade ID parameter
 * @returns {Promise<Array>} Imported data
 */
async function importSteadyStateConsumption(
  keycloak,
  plantId,
  year,
  file,
  mode,
  gradeId,
) {
  const baseUrl = `${Config.CaseEngineUrl}/task/steady-state-norms-import`
  const formData = new FormData()
  formData.append('file', file)
  formData.append('plantId', plantId)
  formData.append('year', year)

  if (mode) {
    formData.append('mode', mode)
  }
  if (gradeId) {
    formData.append('gradeId', gradeId)
  }

  const headers = {
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(baseUrl, {
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
 * Calculate Steady State Consumption
 * @param {Object} keycloak - Keycloak session object
 * @param {Number} plantId - Plant ID
 * @param {Number} year - AOP Year
 * @returns {Promise<Array>} Calculated data
 */
async function calculateSteadyStateConsumption(keycloak, plantId, year) {
  const baseUrl = `${Config.CaseEngineUrl}/task/calculate-steady-state-norms`
  const queryParams = new URLSearchParams({ year, plantId })
  const url = `${baseUrl}?${queryParams.toString()}`
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
 * Save Steady State Consumption by Grade (PE/PP vertical)
 * Same as NormalOperationNormsApiService.saveNormalOperationNormsData(..., 'pe', ...)
 * Endpoint: POST /task/steady-state-norms/polyester?year=&plantId=&gradeId=
 */
async function saveGradeWiseSteadyStateConsumption(
  PLANT_ID,
  payload,
  keycloak,
  gradeId,
  AOP_YEAR,
) {
  const queryParams = new URLSearchParams({ year: AOP_YEAR, plantId: PLANT_ID })
  if (gradeId) queryParams.append('gradeId', gradeId)
  const url = `${Config.CaseEngineUrl}/task/steady-state-norms/polyester?${queryParams.toString()}`
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

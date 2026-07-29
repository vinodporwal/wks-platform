import Config from '../consts'
import { json } from './request'
export const ConsumptionNormsApiService = {
  saveAOPConsumptionNorm,
  getConsumptionAOPNormsGrades,
  getConsumptionNormsData,
  handleCalculateConsumptionNorms,
  OverallConsumptionPEPPExport,
  getProposedNormsData,
  saveProposedNormsData,
  ProposedConsumptionNormsExport,
  proposedNormsImport,
  slowdownconsumptionExportMEG,
  ExcelSlowdownConsumptionMEG,
  getProposedAOPNormsGrades,
}
async function saveAOPConsumptionNorm(PLANT_ID, shutdownDetails, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/overall-consumption`
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
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function getConsumptionAOPNormsGrades(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/consumption-aop/grades?year=${AOP_YEAR}&plantId=${PLANT_ID}`
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

async function getConsumptionNormsData(keycloak, gradeId, PLANT_ID, AOP_YEAR) {
  const year = AOP_YEAR
  const plantId = PLANT_ID
  // Construct URL based on presence of gradeId
  const baseUrl = `${Config.CaseEngineUrl}/task/overall-consumption`
  const queryParams = new URLSearchParams({
    plantId,
    year,
  })
  if (gradeId) {
    queryParams.append('gradeId', gradeId)
  }
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

async function handleCalculateConsumptionNorms(PLANT_ID, AOP_YEAR, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/calculate-overall-consumption?year=${AOP_YEAR}&plantId=${PLANT_ID}`
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

export async function OverallConsumptionPEPPExport(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
  SCREEN_NAME,
) {
  const url = `${Config.CaseEngineUrl}/task/overall-consumption-export?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
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
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_${SCREEN_NAME}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Shutdown Excel:', e)
    return Promise.reject(e)
  }
}

async function getProposedNormsData(keycloak, gradeId, PLANT_ID, AOP_YEAR) {
  const year = AOP_YEAR
  const plantId = PLANT_ID
  // Construct URL based on presence of gradeId
  const baseUrl = `${Config.CaseEngineUrl}/task/proposed-consumption`
  const queryParams = new URLSearchParams({
    plantId,
    year,
  })
  if (gradeId) {
    queryParams.append('gradeId', gradeId)
  }
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

async function saveProposedNormsData(PLANT_ID, AOP_YEAR, payload, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/proposed-consumption?year=${encodeURIComponent(AOP_YEAR)}&plantId=${encodeURIComponent(PLANT_ID)}`
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
export async function ProposedConsumptionNormsExport(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
  SCREEN_NAME,
) {
  const url = `${Config.CaseEngineUrl}/task/proposed-consumption-export?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
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
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_${SCREEN_NAME}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Proposed Consumption Norms Excel:', e)
    return Promise.reject(e)
  }
}
async function proposedNormsImport(file, keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/proposed-consumption-import?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
    return json(keycloak, resp) // assuming `json()` handles response properly
  } catch (e) {
    console.error('Error importing Optimizer Input Excel:', e)
    return await Promise.reject(e)
  }
}
export async function slowdownconsumptionExportMEG(
  keycloak,
  plantId,
  year,
  ExcelName,
) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-consumption-configuration-export?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
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
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${ExcelName}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Shutdown Excel:', e)
    return Promise.reject(e)
  }
}
async function ExcelSlowdownConsumptionMEG(file, keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/slowdown-consumption-configuration-import?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
    return json(keycloak, resp) // assuming `json()` handles response properly
  } catch (e) {
    console.error('Error importing Optimizer Input Excel:', e)
    return await Promise.reject(e)
  }
}
async function getProposedAOPNormsGrades(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/proposed-aop/grades?year=${AOP_YEAR}&plantId=${PLANT_ID}`
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
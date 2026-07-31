import Config from '../consts'
import { json } from './request'
export const ProductionConstarintsApiService = {
  getProductionConstraints,
  getAopBasiswithStartDate,
  postProductionConstraints,
  exportExcelProductionConstraints,
  importExcelProductionConstraints,
  saveProductionConstraint,
  saveAopBasiswithStartDate,
}

async function getAopBasiswithStartDate(keycloak, PLANT_ID, AOP_YEAR, TYPE) {
  let url = `${Config.CaseEngineUrl}/task/data-config?year=${AOP_YEAR}&plantFKId=${PLANT_ID}`
  if (TYPE) {
    url += `&type=${TYPE}`
  }
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

async function getProductionConstraints(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  TYPE,
  isCrackerC2 = false,
) {
  if (isCrackerC2) {
    return getAopBasiswithStartDate(keycloak, PLANT_ID, AOP_YEAR, TYPE)
  }
  const url = `${Config.CaseEngineUrl}/task/production-configuration-basis?year=${AOP_YEAR}&plantFKId=${PLANT_ID}&type=${TYPE}`
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
async function postProductionConstraints(PLANT_ID, shutdownDetails, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/production-constraints?year=${AOP_YEAR}&plantFKId=${PLANT_ID}`
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

export async function exportExcelProductionConstraints(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
  SCREEN_NAME,
) {
  const url = `${Config.CaseEngineUrl}/task/production-constraints-export?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
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
async function importExcelProductionConstraints(
  file,
  keycloak,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/production-constraints-import?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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

async function saveAopBasiswithStartDate(
  PLANT_ID,
  turnAroundDetails,
  keycloak,
  AOP_YEAR,
) {
  var url = `${Config.CaseEngineUrl}/task/data-config?year=${AOP_YEAR}&plantFKId=${PLANT_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(turnAroundDetails),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function saveProductionConstraint(
  PLANT_ID,
  turnAroundDetails,
  keycloak,
  AOP_YEAR,
) {
  var url = `${Config.CaseEngineUrl}/task/production-configuration-basis?year=${AOP_YEAR}&plantFKId=${PLANT_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(turnAroundDetails),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

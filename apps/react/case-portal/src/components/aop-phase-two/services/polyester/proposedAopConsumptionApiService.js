import Config from 'consts/index'
import { json } from 'services/request'

export const ProposedAopConsumptionApiService = {
  getGrades,
  getProposedAopConsumption,
  saveProposedAopConsumption,
  exportProposedAopConsumption,
  importProposedAopConsumption,
  calculateProposedAopConsumption,
}

// ========================|| Proposed AOP Consumption APIs ||=====================================//

async function getGrades(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/consumption-aop/grades?year=${year}&plantId=${plantId}`
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

async function getProposedAopConsumption(keycloak, gradeId, plantId, year) {
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

async function saveProposedAopConsumption(keycloak, plantId, year, payload) {
  const url = `${Config.CaseEngineUrl}/task/proposed-consumption?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
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

async function exportProposedAopConsumption(
  keycloak,
  plantId,
  year,
  excelExportTitle,
  screenName,
) {
  const url = `${Config.CaseEngineUrl}/task/proposed-consumption-export?year=${encodeURIComponent(year)}&plantId=${encodeURIComponent(plantId)}`
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

async function importProposedAopConsumption(keycloak, plantId, year, file) {
  const url = `${Config.CaseEngineUrl}/task/proposed-consumption-import?plantId=${plantId}&year=${year}`
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

async function calculateProposedAopConsumption(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/calculate-overall-consumption?year=${year}&plantId=${plantId}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return await resp.json()
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

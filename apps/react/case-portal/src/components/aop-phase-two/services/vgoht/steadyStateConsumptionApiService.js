import Config from 'consts/index'
import { json } from 'services/request'

export const SteadyStateConsumptionApiService = {
  getSteadyStateConsumption,
  saveSteadyStateConsumption,
  exportSteadyStateConsumption,
  importSteadyStateConsumption,
  calculateSteadyStateConsumption,
}

// ========================|| Steady State Consumption APIs ||=====================================//

async function getSteadyStateConsumption(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/steady-state-norms?year=${year}&plantId=${plantId}`
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

async function saveSteadyStateConsumption(keycloak, plantId, year, data) {
  const baseUrl = `${Config.CaseEngineUrl}/task/steady-state-norms`
  const queryParams = new URLSearchParams({
    year,
    plantId,
  })
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

    // Check for HTTP errors before parsing response
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

async function exportSteadyStateConsumption(
  keycloak,
  plantId,
  year,
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
    return await resp.blob()
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

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

async function calculateSteadyStateConsumption(keycloak, plantId, year) {
  const baseUrl = `${Config.CaseEngineUrl}/task/vgoht/calculate-steady-state-norms`
  const queryParams = new URLSearchParams({
    year,
    plantId,
  })

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

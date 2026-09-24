import Config from 'consts/index'
import { json } from 'services/request'

export const EfficiencyAndFuelRatioAPIService = {
  getEfficiency,
  saveEfficiency,
  getFuelRatio,
  saveFuelRatio,
}

// ===================== GENERIC HELPERS ===================== //

function buildHeaders(keycloak) {
  return {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
}

function buildPlantIdsParam(plantIds) {
  const plantIdArray = Array.isArray(plantIds) ? plantIds : [plantIds]
  return plantIdArray.join(',')
}

// ===================== || EFFICIENCY GET || ===================== //
// GET /task/jmd/efficiency?plantIds=...&aopYear=...
async function getEfficiency(keycloak, plantIds, aopYear) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/efficiency?plantIds=${queryParams}&aopYear=${aopYear}`
  const headers = buildHeaders(keycloak)
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching efficiency data:', e)
    return await Promise.reject(e)
  }
}

// ===================== || EFFICIENCY SAVE OR UPDATE || ===================== //
// POST /task/jmd/efficiency?plantIds=...&aopYear=...
async function saveEfficiency(keycloak, plantIds, aopYear, payload) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/efficiency?plantIds=${queryParams}&aopYear=${aopYear}`
  const headers = buildHeaders(keycloak)
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error saving efficiency data:', e)
    return await Promise.reject(e)
  }
}

// ===================== || FUEL RATIO GET || ===================== //
// GET /task/jmd/fuel-ratio?plantIds=...&aopYear=...
async function getFuelRatio(keycloak, plantIds, aopYear) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/fuel-ratio?plantIds=${queryParams}&aopYear=${aopYear}`
  const headers = buildHeaders(keycloak)
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching fuel ratio data:', e)
    return await Promise.reject(e)
  }
}

// ===================== || FUEL RATIO SAVE OR UPDATE || ===================== //
// POST /task/jmd/fuel-ratio?plantIds=...&aopYear=...
async function saveFuelRatio(keycloak, plantIds, aopYear, payload) {
  const queryParams = buildPlantIdsParam(plantIds)
  const url = `${Config.CaseEngineUrl}/task/jmd/fuel-ratio?plantIds=${queryParams}&aopYear=${aopYear}`
  const headers = buildHeaders(keycloak)
  const body = JSON.stringify(payload)
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const result = await json(keycloak, resp)
    return result || { success: true }
  } catch (e) {
    console.error('Error saving fuel ratio data:', e)
    return await Promise.reject(e)
  }
}

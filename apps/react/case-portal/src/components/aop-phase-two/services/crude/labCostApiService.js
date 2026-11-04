import Config from 'consts/index'
import { json } from 'services/request'

export const LabCostApiService = {
  getLabCostData,
  getLabCostCostCenterDropdown,
  getLabCostMaterialDropdown,
  saveLabCostData,
  deleteLabCostData,
}

/**
 * 1) GET API - SP_GetLabCostData
 */
async function getLabCostData(keycloak, year) {
  const url = `${Config.CaseEngineUrl}/task/lab-cost?aopYear=${encodeURIComponent(year || '')}`
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

/**
 * 2) Dropdown API - Lab Cost Cost Center Dropdown
 */
async function getLabCostCostCenterDropdown(keycloak) {
  const url = `${Config.CaseEngineUrl}/task/lab-cost-cost-center-dropdown`
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

/**
 * 3) Dropdown API - Lab Cost Material Dropdown
 */
async function getLabCostMaterialDropdown(keycloak) {
  const url = `${Config.CaseEngineUrl}/task/lab-cost-material-dropdown`
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

/**
 * 4) SAVE API - Insert or Update Lab Cost records
 */
async function saveLabCostData(payload, keycloak, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/lab-cost?aopYear=${encodeURIComponent(aopYear || '')}`
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
 * 5) DELETE API - Delete Lab Cost transaction by masterId and AOP year
 */
async function deleteLabCostData(keycloak, masterId, year) {
  const url = `${Config.CaseEngineUrl}/task/lab-cost?masterId=${encodeURIComponent(masterId || '')}&aopYear=${encodeURIComponent(year || '')}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'DELETE', headers })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}


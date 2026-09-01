import Config from 'consts/index'
import { json } from 'services/request'

export const JswBudgetSourceAPIService = {
  getJswBudgetSourceData,
  saveJswBudgetSourceData,
  getDropdownUnit,
  deleteJwBudgetData,
  deleteJswBudgetSourceData,
}

async function getJswBudgetSourceData(keycloak, SITE_ID, year, siteName) {
  let url = `${Config.CaseEngineUrl}/task/profit-center-data?siteId=${SITE_ID}&aopYear=${year}${siteName ? `&siteName=${encodeURIComponent(siteName)}` : ''}`
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
    console.error('Error fetching profit center data:', e)
    return Promise.reject(e)
  }
}

async function saveJswBudgetSourceData(payload, keycloak, year) {
  const url = `${Config.CaseEngineUrl}/task/profit-center-data?aopYear=${year}`
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
    console.error('Error saving profit center data:', e)
    return Promise.reject(e)
  }
}

async function getDropdownUnit(keycloak, SITE_ID, siteName) {
  let url = `${Config.CaseEngineUrl}/task/unit-dropdown?siteId=${SITE_ID}${siteName ? `&siteName=${encodeURIComponent(siteName)}` : ''}`
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
    console.error('Error fetching unit dropdown:', e)
    return Promise.reject(e)
  }
}

async function deleteJwBudgetData(keycloak, id, year) {
  const url = `${Config.CaseEngineUrl}/task/profit-center-data?id=${encodeURIComponent(id)}&aopYear=${encodeURIComponent(year)}`
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
    console.error('Error deleting profit center data:', e)
    return Promise.reject(e)
  }
}

async function deleteJswBudgetSourceData(keycloak, id, year) {
  return deleteJwBudgetData(keycloak, id, year)
}
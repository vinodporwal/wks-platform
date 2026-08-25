import Config from 'consts/index'
import { json } from 'services/request'

export const ThroughputNormsApiService = {
  getThroughputNorms,
  saveThroughputNorms,
  getNormsMaterialDropdown,
  getDropdownUnit,
  deleteThroughputNormsData,
  deleteThroughputNorms,
}

async function getThroughputNorms(keycloak, siteName = 'SEZ', year, SITE_ID = '') {
  let url = `${Config.CaseEngineUrl}/task/throughput-norms?siteName=${encodeURIComponent(siteName)}&aopYear=${encodeURIComponent(year || '')}`
  if (SITE_ID) {
    url += `&siteId=${encodeURIComponent(SITE_ID)}`
  }
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
    console.error('Error fetching throughput norms data:', e)
    return Promise.reject(e)
  }
}

async function saveThroughputNorms(keycloak, payload, year) {
  const url = `${Config.CaseEngineUrl}/task/throughput-norms?aopYear=${year}`
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
    console.error('Error saving throughput norms data:', e)
    return Promise.reject(e)
  }
}

async function getNormsMaterialDropdown(keycloak, siteName = 'SEZ', SITE_ID = '') {
  let url = `${Config.CaseEngineUrl}/task/norms-material-dropdown?siteName=${encodeURIComponent(siteName)}`
  if (SITE_ID) {
    url += `&siteId=${encodeURIComponent(SITE_ID)}`
  }
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
    console.error('Error fetching norms material dropdown:', e)
    return Promise.reject(e)
  }
}

async function getDropdownUnit(keycloak, SITE_ID, siteName = 'SEZ') {
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

async function deleteThroughputNormsData(keycloak, materialId, unitId, year) {
  const url = `${Config.CaseEngineUrl}/task/throughput-norms?materialId=${encodeURIComponent(materialId || '')}&unitId=${encodeURIComponent(unitId || '')}&aopYear=${encodeURIComponent(year || '')}`
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
        `Failed to delete throughput norms data: ${resp.status} ${resp.statusText}`,
      )
    }
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error deleting throughput norms data:', e)
    return Promise.reject(e)
  }
}

async function deleteThroughputNorms(keycloak, materialId, unitId, year) {
  return deleteThroughputNormsData(keycloak, materialId, unitId, year)
}

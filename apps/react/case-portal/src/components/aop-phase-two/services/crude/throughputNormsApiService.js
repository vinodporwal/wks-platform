import Config from 'consts/index'
import { json } from 'services/request'

export const ThroughputNormsApiService = {
  getThroughputNorms,
  saveThroughputNorms,
  getNormsMaterialDropdown,
  getDropdownUnit,
  deleteThroughputNormsData,
}

async function getThroughputNorms(keycloak, SITE_ID, year) {
  let url = `${Config.CaseEngineUrl}/task/throughput-norms?siteId=${SITE_ID}&aopYear=${year}`
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

async function getNormsMaterialDropdown(keycloak, SITE_ID, profitId) {
  let url = `${Config.CaseEngineUrl}/task/norms-material-dropdown?siteId=${SITE_ID}`
  if (profitId) {
    url += `&profitId=${profitId}`
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

async function getDropdownUnit(keycloak, SITE_ID) {
  let url = `${Config.CaseEngineUrl}/task/profit-center-uom-dropdown?siteId=${SITE_ID}`
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
  const url = `${Config.CaseEngineUrl}/task/throughput-norms?materialId=${encodeURIComponent(materialId || '')}&unitId=${encodeURIComponent(unitId || '')}&aopYear=${year}`
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
    return await resp.text()
  } catch (e) {
    console.error('Error deleting throughput norms data:', e)
    return Promise.reject(e)
  }
}

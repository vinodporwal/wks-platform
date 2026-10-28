import Config from 'consts/index'
import { json } from 'services/request'

export const ManualExclusionDateApiService = {
  getManualExclusionDate,
  postManualExclusionDate,
  deleteManualExclusionDate,
}

async function getManualExclusionDate(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/manual-exclusion-dates?year=${year}&plantFKId=${plantId}`

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

async function postManualExclusionDate(payload, keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/manual-exclusion-dates?year=${year}&plantFKId=${plantId}`
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

async function deleteManualExclusionDate(keycloak, payload) {
  const url = `${Config.CaseEngineUrl}/task/manual-exclusion-dates`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers,
      body: JSON.stringify(payload)
    })
    if (!resp.ok) {
      throw new Error(
        `Failed to delete data: ${resp.status} ${resp.statusText}`,
      )
    }
    return await resp.text()
  } catch (e) {
    console.error('Error deleting data:', e)
    return Promise.reject(e)
  }
}

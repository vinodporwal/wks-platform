import Config from '../consts'
import { json } from './request'

export const FurnaceMaintenanceActivityApiService = {
  getFurnaceMaintenanceActivity,
  saveFurnaceMaintenanceActivity,
  deleteFurnaceMaintenanceActivity,
  getFurnaceDropdownData,
}

export async function getFurnaceMaintenanceActivity(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/furnace-maintenance-activities?plantId=${PLANT_ID}&aopYear=${AOP_YEAR}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error(e)
    return Promise.reject(e)
  }
}

export async function saveFurnaceMaintenanceActivity(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  data,
) {
  const url = `${Config.CaseEngineUrl}/task/furnace-maintenance-activity?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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
    return json(keycloak, resp)
  } catch (e) {
    console.error(e)
    return Promise.reject(e)
  }
}

export async function deleteFurnaceMaintenanceActivity(id, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/furnace-maintenance-activity?id=${id}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'DELETE', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error(e)
    return Promise.reject(e)
  }
}

export async function getFurnaceDropdownData(keycloak, PLANT_ID) {
  const url = `${Config.CaseEngineUrl}/task/furnace-dropdown?plantId=${PLANT_ID}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error(e)
    return Promise.reject(e)
  }
}

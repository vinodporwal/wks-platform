import Config from '../consts'
import { json } from './request'
export const SpecificConsumptionService = {
  getSpecificConsumptionII,
  saveSpecificConsumptionII,
  getCombinedMCU,
  getCombinedMCUDetails,
}
export async function getSpecificConsumptionII(
  keycloak,
  reportType,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/specific-consumption-t17?plantId=${PLANT_ID}&year=${AOP_YEAR}&reportType=${reportType}`
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
async function saveSpecificConsumptionII(
  keycloak,
  workFlowData,
  PLANT_ID,
  AOP_YAER,
) {
  const url = `${Config.CaseEngineUrl}/task/specific-consumption-t17?plantId=${PLANT_ID}&year=${AOP_YAER}`
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(workFlowData),
    })
    return json(keycloak, resp)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

export async function getCombinedMCU(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/c3-calculation?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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

export async function getCombinedMCUDetails(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/c3-detail?plantId=${PLANT_ID}&year=${AOP_YEAR}`
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

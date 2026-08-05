import Config from '../consts'
import { json } from './request'

export const OptimizingVariablesApiService = {
  getCrackerC2OptimizingVariables,
  saveCrackerC2OptimizingVariables,
  getCrackerC2OptimizingVariablesDropdown,
  getFeedTypeFlowMappings,
}

async function getCrackerC2OptimizingVariables(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/optimizing-variables-dropdown?plantId=${PLANT_ID}&aopYear=${AOP_YEAR}`
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

async function saveCrackerC2OptimizingVariables(
  PLANT_ID,
  payload,
  keycloak,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/optimizing-variables-dropdown?plantId=${PLANT_ID}&aopYear=${AOP_YEAR}`
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

async function getCrackerC2OptimizingVariablesDropdown(keycloak) {
  const url = `${Config.CaseEngineUrl}/task/cracker-c2-optimizing-variables-dropdown`
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

async function getFeedTypeFlowMappings(keycloak, plantId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/feed-type-flow-mappings?plantId=${plantId}&aopYear=${aopYear}`
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

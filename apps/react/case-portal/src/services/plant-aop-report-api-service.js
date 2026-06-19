import Config from '../consts'
import { json } from './request'
export const PlantAopReportApiService = {
  getPlantsafetyPerformance,
  savePlantsafetyPerformance,
  getSafetyImprovementInitiative,
  saveSafetyImprovementInitiative,
}
async function getPlantsafetyPerformance(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/plant-report?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}`
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
    return Promise.reject(e)
  }
}
async function savePlantsafetyPerformance(keycloak, payload) {
  const url = `${Config.CaseEngineUrl}/task/plant-report`
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
    return Promise.reject(e)
  }
}

async function getSafetyImprovementInitiative(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/plant-safety-improvement?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}`
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
    return Promise.reject(e)
  }
}
async function saveSafetyImprovementInitiative(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  payload,
) {
  const url = `${Config.CaseEngineUrl}/task/plant-safety-improvement?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}`
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
    return Promise.reject(e)
  }
}

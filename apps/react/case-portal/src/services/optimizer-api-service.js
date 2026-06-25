import Config from '../consts'
import { json } from './request'

export const OptimizerDataApiService = {
  fetchModes,
  getGradeMixOptimizerConstant,
  saveGradeMixOptimizerConstant,
  getCalculatedProposedBusinessOptimizer,
  getGradewiseBudgetOperatingHours,
  saveGradeBudgetOperatingHours,
  calculateGradeMixBudgetOpeartingHours,
}
async function fetchModes(keycloak, PLANT_ID, AOP_YEAR, TYPE) {
  const url = `${Config.CaseEngineUrl}/task/modes?year=${AOP_YEAR}&plantId=${PLANT_ID}&type=${TYPE}`
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
async function getGradeMixOptimizerConstant(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/grade-mix-optimizer-constants?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}`
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

async function saveGradeMixOptimizerConstant(PLANT_ID, payload, keycloak, AOP_YEAR) {
  var url = `${Config.CaseEngineUrl}/task/production-norms?year=${AOP_YEAR}&plantFKId=${PLANT_ID}`
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

async function getCalculatedProposedBusinessOptimizer(keycloak, PLANT_ID, AOP_YEAR, lineId) {
  const url = `${Config.CaseEngineUrl}/task/calculated-proposed-business-demand?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}&lineId=${lineId}`
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
async function getGradewiseBudgetOperatingHours(keycloak, PLANT_ID, AOP_YEAR,lineId) {
  const url = `${Config.CaseEngineUrl}/task/budgeted-operating-hours-data?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}&lineId=${lineId}`
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
async function saveGradeBudgetOperatingHours(PLANT_ID, payload, keycloak, AOP_YEAR,lineId) {
  var url = `${Config.CaseEngineUrl}/task/budgeted-operating-hours-data?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}&lineId=${lineId}`
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
async function calculateGradeMixBudgetOpeartingHours(PLANT_ID, AOP_YEAR, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/calculate-budget-operation-hours?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`HTTP error! Status: ${resp.status}`)
    }
    const data = await resp.json()
    return data
  } catch (e) {
    console.error('Error fetching calculation data:', e)
    return Promise.reject(e)
  }
}
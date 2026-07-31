import Config from '../consts'
import { json } from './request'

export const ProductionSchedulingApiService = {
  getProductionSchedulingData,
  updateProductionSchedulingData,
  getProductionSchedulingTransactionData,
  handleCalculateProductionSchedulingData,
}

async function getProductionSchedulingData(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/prod-scheduling-config?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}`
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
async function updateProductionSchedulingData(
  PLANT_ID,
  payload,
  keycloak,
  AOP_YEAR,
) {
  var url = `${Config.CaseEngineUrl}/task/prod-scheduling-config?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}`
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
async function getProductionSchedulingTransactionData(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/prod-scheduling?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}`
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
async function handleCalculateProductionSchedulingData(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
) {
  const url = `${Config.CaseEngineUrl}/task/calculate-prod-scheduling?aopYear=${AOP_YEAR}&plantId=${PLANT_ID}`
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

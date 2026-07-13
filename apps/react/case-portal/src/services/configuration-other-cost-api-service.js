import Config from '../consts'
import { json } from './request'

export const ConfigurationOtherCostApiService = {
  getConfigurationOtherCostData,
  saveConfigurationOtherCostData,
}

async function getConfigurationOtherCostData(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/configuration-other-cost?year=${AOP_YEAR}&plantFKId=${PLANT_ID}`
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

async function saveConfigurationOtherCostData(PLANT_ID, payload, keycloak, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/configuration-other-cost?year=${AOP_YEAR}&plantFKId=${PLANT_ID}`
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

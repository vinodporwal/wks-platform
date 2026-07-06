import Config from '../consts'
import { json } from './request'

export const ProposedAopApiService = {
  getProposedAOP,
  calculateProposedAOP,
  saveProposedAOP,
}

async function getProposedAOP(keycloak, plantId, aopYear, gradeId) {
  const url = `${Config.CaseEngineUrl}/task/proposed-aop?year=${aopYear}&plantId=${plantId}&gradeId=${gradeId}`
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
    return await Promise.reject(e)
  }
}

async function calculateProposedAOP(keycloak, plantId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/calculate-proposed-aop?plantId=${plantId}&aopYear=${aopYear}`
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
    return await Promise.reject(e)
  }
}

async function saveProposedAOP(keycloak, payload) {
  const url = `${Config.CaseEngineUrl}/task/save-proposed-aop`
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
    console.error(e)
    return await Promise.reject(e)
  }
}

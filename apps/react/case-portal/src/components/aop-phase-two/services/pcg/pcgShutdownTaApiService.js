import Config from 'consts/index'
import { json } from 'services/request'

export const PCGShutdownTaApiService = {
  getShutdownTaTransactions,
  getShutdownTaGasifierDropdown,
  saveShutdownTaTransactions,
  getAopProductionNorms,
}

/**
 * Get Gasifier Dropdown
 * GET /gasifier-dropdown?plantId=&aopYear=
 */
async function getShutdownTaGasifierDropdown(keycloak, plantId, aopYear) {
  let url = `${Config.CaseEngineUrl}/task/gasifier-dropdown`
  const params = []
  if (plantId) params.push(`plantId=${encodeURIComponent(plantId)}`)
  if (aopYear) params.push(`aopYear=${encodeURIComponent(aopYear)}`)
  if (params.length > 0) url += `?${params.join('&')}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching gasifier dropdown:', e)
    return Promise.reject(e)
  }
}

/**
 * Get Shutdown/TA Transactions
 * GET /shutdown-transactions?plantId=&aopYear=
 */
async function getShutdownTaTransactions(keycloak, plantId, aopYear) {
  let url = `${Config.CaseEngineUrl}/task/shutdown-transactions`
  const params = []
  if (plantId) params.push(`plantId=${encodeURIComponent(plantId)}`)
  if (aopYear) params.push(`aopYear=${encodeURIComponent(aopYear)}`)
  if (params.length > 0) url += `?${params.join('&')}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching shutdown transactions:', e)
    return Promise.reject(e)
  }
}

/**
 * Save Shutdown/TA Transactions
 * POST /shutdown-transactions?plantId=
 * Body: List<ShutdownTaTransactionDTO>
 */
async function saveShutdownTaTransactions(keycloak, plantId, payload) {
  let url = `${Config.CaseEngineUrl}/task/shutdown-transactions`
  if (plantId) url += `?plantId=${encodeURIComponent(plantId)}`

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
    console.error('Error saving shutdown transactions:', e)
    return Promise.reject(e)
  }
}

/**
 * Get AOP Production Norms
 * GET /task/aop-production-norms?plantId=&aopYear=
 */
async function getAopProductionNorms(keycloak, plantId, aopYear) {
  let url = `${Config.CaseEngineUrl}/task/aop-production-norms`
  const params = []
  if (plantId) params.push(`plantId=${encodeURIComponent(plantId)}`)
  if (aopYear) params.push(`aopYear=${encodeURIComponent(aopYear)}`)
  if (params.length > 0) url += `?${params.join('&')}`

  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error fetching AOP production norms:', e)
    return Promise.reject(e)
  }
}

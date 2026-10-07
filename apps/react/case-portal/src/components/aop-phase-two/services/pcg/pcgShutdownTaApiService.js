import Config from 'consts/index'
import { json } from 'services/request'

export const PCGShutdownTaApiService = {
  getShutdownTaTransactions,
  getShutdownTaGasifierDropdown,
}

/**
 * Get Shutdown/TA Transactions
 * GET /task/shutdown-ta-transactions?plantId=&year=
 */
async function getShutdownTaTransactions(keycloak, plantId, year) {
  let url = `${Config.CaseEngineUrl}/task/shutdown-ta-transactions`
  const params = []
  if (plantId) params.push(`plantId=${encodeURIComponent(plantId)}`)
  if (year) params.push(`year=${encodeURIComponent(year)}`)
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
    console.error('Error fetching shutdown TA transactions:', e)
    return await Promise.reject(e)
  }
}

/**
 * Get Gasifier Dropdown for Shutdown/TA
 * GET /task/shutdown-ta-gasifier-dropdown?plantId=&year=
 */
async function getShutdownTaGasifierDropdown(keycloak, plantId, year) {
  let url = `${Config.CaseEngineUrl}/task/shutdown-ta-gasifier-dropdown`
  const params = []
  if (plantId) params.push(`plantId=${encodeURIComponent(plantId)}`)
  if (year) params.push(`year=${encodeURIComponent(year)}`)
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
    console.error('Error fetching shutdown TA gasifier dropdown:', e)
    return await Promise.reject(e)
  }
}

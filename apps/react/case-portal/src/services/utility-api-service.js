import Config from '../consts'
import { json } from './request'
export const UtilityApiService = {
  handleCalculateCombined,
}

async function handleCalculateCombined(plantId, year, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/calculate-combine?plantId=${plantId}&aopYear=${year}`
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

import Config from 'consts/index'
import { json } from 'services/request'

export const ProductGradeSelectionApiService = {
  getGradeSelection,
  saveGradeSelection,
}

async function getGradeSelection(keycloak, plantId, year) {
  const url = `${Config.CaseEngineUrl}/task/grade-selection?plantId=${plantId}&year=${year}`
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

async function saveGradeSelection(keycloak, payload, year) {
  const url = `${Config.CaseEngineUrl}/task/grade-selection?year=${year}`
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

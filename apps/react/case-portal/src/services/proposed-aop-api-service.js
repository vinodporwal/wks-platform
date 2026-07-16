import Config from '../consts'
import { json } from './request'

export const ProposedAopApiService = {
  getProposedAOP,
  calculateProposedAOP,
  saveProposedAOP,
  exportProposedAOP,
  importProposedAOP,
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

async function exportProposedAOP(
  keycloak,
  plantId,
  year,
  EXCEL_EXPORT_TITLE,
  SCREEN_NAME,
) {
  const url = `${Config.CaseEngineUrl}/task/proposed-aop-export?plantId=${encodeURIComponent(plantId)}&year=${encodeURIComponent(year)}`
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'GET',
      headers,
    })
    if (!resp.ok) {
      throw new Error(`Export failed: ${resp.status} ${resp.statusText}`)
    }
    const blob = await resp.blob()
    const urlBlob = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = urlBlob
    a.download = `${EXCEL_EXPORT_TITLE}_${SCREEN_NAME}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Proposed AOP Excel:', e)
    return Promise.reject(e)
  }
}

async function importProposedAOP(file, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/proposed-aop-import`
  const formData = new FormData()
  formData.append('file', file)

  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'POST',
      headers,
      body: formData,
    })
    return json(keycloak, resp)
  } catch (e) {
    console.error('Error importing Proposed AOP Excel:', e)
    return await Promise.reject(e)
  }
}


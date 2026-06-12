import Config from '../consts'
import { json } from './request'

export const CatalystChangeOverApiDataService = {
  postCatalystChangeOver,
  getCatalystChangeOver,
  exportCatalystChangeOver,
  importCatalystChangeOver,
  deleteCatalystChangeOver,
}

async function getCatalystChangeOver(keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/catalyst-change-over?year=${AOP_YEAR}&plantId=${PLANT_ID}`

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

async function postCatalystChangeOver(payload, keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/catalyst-change-over/${AOP_YEAR}`
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

async function exportCatalystChangeOver(
  keycloak,
  PLANT_ID,
  AOP_YEAR,
  EXCEL_EXPORT_TITLE,
) {
  const url = `${Config.CaseEngineUrl}/task/catalyst-change-over-export?year=${AOP_YEAR}&plantId=${PLANT_ID}`
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
    a.download = `${EXCEL_EXPORT_TITLE}_Catalyst_Changeover.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(urlBlob)
  } catch (e) {
    console.error('Error exporting Catalyst_Changeover Excel:', e)
    return Promise.reject(e)
  }
}

async function importCatalystChangeOver(file, keycloak, PLANT_ID, AOP_YEAR) {
  const url = `${Config.CaseEngineUrl}/task/catalyst-change-over-import?year=${AOP_YEAR}&plantId=${PLANT_ID}`
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
    return await resp.json()
  } catch (e) {
    console.error('Error importing Excel:', e)
    return Promise.reject(e)
  }
}

async function deleteCatalystChangeOver(deleteId, keycloak) {
  const url = `${Config.CaseEngineUrl}/task/catalyst-change-over/${deleteId}`
  const headers = {
    Accept: 'application/json',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, {
      method: 'DELETE',
      headers,
    })
    if (!resp.ok) {
      throw new Error(
        `Failed to delete data: ${resp.status} ${resp.statusText}`,
      )
    }
    return await resp.text()
  } catch (e) {
    console.error('Error deleting data:', e)
    return Promise.reject(e)
  }
}

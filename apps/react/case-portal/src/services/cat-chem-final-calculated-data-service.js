import Config from '../consts'
import { json } from './request'

export const CatChemFinalCalculatedDataService = {
  getCatChemFinalCalculatedData,
  exportCatChemFinalCalculatedExcel,
  handleCatChemFinalCalculatedData,
}

async function getCatChemFinalCalculatedData(keycloak, plantId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/final-calculated-cat-chem?plantId=${plantId}&aopYear=${aopYear}`
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

async function exportCatChemFinalCalculatedExcel(
  keycloak,
  plantId,
  aopYear,
  fileName,
) {
  const url = `${Config.CaseEngineUrl}/task/final-calculated-cat-chem-export?plantId=${plantId}&aopYear=${aopYear}`
  const headers = {
    Accept: 'application/octet-stream',
    Authorization: `Bearer ${keycloak.token}`,
  }
  try {
    const resp = await fetch(url, { method: 'GET', headers })
    if (!resp.ok) throw new Error('Export failed')
    const blob = await resp.blob()
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.setAttribute('download', `${fileName}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(link.href)
  } catch (e) {
    console.log(e)
    return await Promise.reject(e)
  }
}

async function handleCatChemFinalCalculatedData(keycloak, plantId, aopYear) {
  const url = `${Config.CaseEngineUrl}/task/calculate-final-calculated-cat-chem?plantId=${plantId}&aopYear=${aopYear}`
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
